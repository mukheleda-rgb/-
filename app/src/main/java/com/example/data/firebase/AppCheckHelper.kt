package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

@Volatile
private var appCheckFactoryInstalled = false
private val appCheckLock = Any()

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        val appContext = context.applicationContext ?: context
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext) ?: run {
                Log.w(TAG, "FirebaseApp initialization returned null; skipping App Check setup.")
                return
            }
        }

        if (BuildConfig.DEBUG) {
            val debugToken = try {
                intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            } catch (_: Throwable) {
                null
            }

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = FirebaseApp.getInstance().persistenceKey
                val prefs = appContext.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // Synchronous commit required before DebugAppCheckProviderFactory reads preferences
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            }
        }

        synchronized(appCheckLock) {
            if (appCheckFactoryInstalled) return
            if (BuildConfig.DEBUG) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                appCheckFactoryInstalled = true
                Log.d(TAG, "DebugAppCheckProviderFactory installed.")
            } else {
                val resId = appContext.resources.getIdentifier(
                    RECAPTCHA_SITE_KEY_RES,
                    "string",
                    appContext.packageName
                )
                if (resId != 0) {
                    val siteKey = appContext.getString(resId)
                    if (siteKey.isNotBlank()) {
                        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                        )
                        appCheckFactoryInstalled = true
                        Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                    } else {
                        Log.e(TAG, "recaptcha_site_key resource string is blank.")
                    }
                } else {
                    Log.e(TAG, "recaptcha_site_key string resource not found in package.")
                }
            }
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to initialize Firebase App Check", t)
    }
}
