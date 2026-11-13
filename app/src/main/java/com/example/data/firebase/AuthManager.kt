package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "Auth"

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val existingUser = try {
        Firebase.auth.currentUser
    } catch (t: Throwable) {
        Log.w(TAG, "FirebaseAuth not ready during auto sign-in: ${t.message}")
        null
    }
    if (existingUser != null) {
        onAuthSuccess()
        return
    }

    val activity = context.findActivity()
    if (credentialManager == null || activity == null || activity.isFinishing || activity.isDestroyed) {
        onUnauthenticated()
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }
    if (clientId == null) {
        onUnauthenticated()
        return
    }

    val request = try {
        // Single credential option per request for silent auto-sign-in
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    } catch (t: Throwable) {
        Log.w(TAG, "Unable to build auto sign-in request: ${t.message}")
        onUnauthenticated()
        return
    }

    scope.launch {
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                onUnauthenticated()
                return@launch
            }
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager?,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val activity = context.findActivity()
    if (credentialManager == null || activity == null || activity.isFinishing || activity.isDestroyed) {
        onAuthError("Unable to open Google Sign-In dialog from current window")
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id).takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }
    if (clientId == null) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val request = try {
        // Single credential option per request for interactive Google Sign-In
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
    } catch (t: Throwable) {
        onAuthError(t.localizedMessage ?: "Failed to prepare Google Sign-In")
        return
    }

    scope.launch {
        try {
            if (activity.isFinishing || activity.isDestroyed) {
                onAuthCancelled()
                return@launch
            }
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (t: Throwable) {
            Log.e(TAG, "Google Sign-In failed", t)
            onAuthError(t.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager?,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    try {
        Firebase.auth.signOut()
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to sign out from Firebase", t)
    }
    scope.launch {
        try {
            credentialManager?.clearCredentialState(ClearCredentialStateRequest())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to clear credential state", t)
        } finally {
            onSignOutComplete()
        }
    }
}
