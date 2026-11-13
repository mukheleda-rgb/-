package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

/**
 * GPU-driven tab swipe controller that keeps a single source of truth for [activeRoute]
 * while exposing [visualPosition] strictly for draw/graphicsLayer phases (0 recompositions
 * and 0 layout passes per swipe frame).
 */
@Stable
class SmoothTabSwipeDeckState(
    initialTabs: List<String>,
    initialRoute: String
) {
    private var tabRoutes: List<String> by mutableStateOf(initialTabs)

    val tabs: List<String>
        get() = tabRoutes

    var activeIndex: Int by mutableIntStateOf(
        initialTabs.indexOf(initialRoute).takeIf { it >= 0 } ?: 0
    )
        private set

    var activeRoute: String by mutableStateOf(
        initialTabs.getOrNull(activeIndex) ?: initialRoute
    )
        private set

    internal var continuousPage: Float by mutableFloatStateOf(activeIndex.toFloat())
    internal var isDragging: Boolean by mutableStateOf(false)
    internal var isSettling: Boolean by mutableStateOf(false)

    internal var jumpFromIndex: Int by mutableIntStateOf(-1)
    internal var jumpToIndex: Int by mutableIntStateOf(-1)
    internal var jumpProgress: Float by mutableFloatStateOf(0f)

    private var dragAnchorIndex: Int = activeIndex
    private var animationJob: Job? = null
    private var animToken: Long = 0L

    /**
     * Live fractional tab index in `[0, tabs.lastIndex]`. Read inside `graphicsLayer`
     * so bottom-bar sliding indicators track the user's finger at 120Hz with 0 recompositions.
     */
    val visualPosition: Float
        get() {
            val maxIdx = (tabRoutes.size - 1).coerceAtLeast(0).toFloat()
            val jFrom = jumpFromIndex
            val jTo = jumpToIndex
            return if (jFrom >= 0 && jTo >= 0 && jFrom != jTo) {
                lerpFloat(jFrom.toFloat(), jTo.toFloat(), jumpProgress).coerceIn(0f, maxIdx)
            } else {
                continuousPage.coerceIn(0f, maxIdx)
            }
        }

    internal val isJumpAnimating: Boolean
        get() = jumpFromIndex >= 0 && jumpToIndex >= 0 && jumpFromIndex != jumpToIndex

    fun syncTabsOrder(newTabs: List<String>) {
        if (newTabs.isEmpty() || newTabs == tabRoutes) return
        tabRoutes = newTabs
        val updatedIdx = newTabs.indexOf(activeRoute).takeIf { it >= 0 } ?: 0
        animToken++
        animationJob?.cancel()
        clearJumpMode()
        isDragging = false
        isSettling = false
        activeIndex = updatedIdx
        activeRoute = newTabs[updatedIdx]
        dragAnchorIndex = updatedIdx
        continuousPage = updatedIdx.toFloat()
    }

    fun navigateToRoute(
        route: String,
        scope: CoroutineScope,
        animate: Boolean = true
    ) {
        val targetIdx = tabRoutes.indexOf(route)
        if (targetIdx < 0) return

        if (!animate) {
            animToken++
            animationJob?.cancel()
            clearJumpMode()
            isDragging = false
            isSettling = false
            activeIndex = targetIdx
            activeRoute = route
            dragAnchorIndex = targetIdx
            continuousPage = targetIdx.toFloat()
            return
        }

        if (
            targetIdx == activeIndex &&
            !isSettling &&
            !isDragging &&
            !isJumpAnimating &&
            abs(continuousPage - targetIdx.toFloat()) < 0.005f
        ) {
            return
        }

        if (isJumpAnimating) {
            continuousPage = if (jumpProgress >= 0.5f) {
                jumpToIndex.toFloat()
            } else {
                jumpFromIndex.toFloat()
            }
            clearJumpMode()
        }

        animToken++
        animationJob?.cancel()
        isDragging = false

        val originFloat = continuousPage
        val originNearestIdx = originFloat.roundToInt().coerceIn(0, tabRoutes.lastIndex)

        activeIndex = targetIdx
        activeRoute = route
        dragAnchorIndex = targetIdx

        // Direct 2-Page GPU Glide when tapping a non-adjacent tab (avoids flashing middle tabs)
        if (abs(targetIdx - originNearestIdx) > 1 && abs(originFloat - originNearestIdx.toFloat()) < 0.08f) {
            jumpFromIndex = originNearestIdx
            jumpToIndex = targetIdx
            jumpProgress = 0f
            isSettling = true
            val token = ++animToken
            animationJob = scope.launch {
                try {
                    val anim = Animatable(0f)
                    anim.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 560f
                        )
                    ) {
                        jumpProgress = value
                    }
                    continuousPage = targetIdx.toFloat()
                } finally {
                    if (animToken == token) {
                        clearJumpMode()
                        isSettling = false
                    }
                }
            }
        } else {
            clearJumpMode()
            isSettling = true
            val token = ++animToken
            animationJob = scope.launch {
                try {
                    val anim = Animatable(originFloat)
                    anim.animateTo(
                        targetValue = targetIdx.toFloat(),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 540f
                        )
                    ) {
                        continuousPage = value
                    }
                    continuousPage = targetIdx.toFloat()
                } finally {
                    if (animToken == token) {
                        isSettling = false
                    }
                }
            }
        }
    }

    internal fun beginDrag() {
        if (isJumpAnimating) {
            val snappedIdx = if (jumpProgress >= 0.5f) jumpToIndex else jumpFromIndex
            continuousPage = snappedIdx.toFloat()
            activeIndex = snappedIdx
            activeRoute = tabRoutes.getOrElse(snappedIdx) { activeRoute }
            clearJumpMode()
        }
        animToken++
        animationJob?.cancel()
        isSettling = false
        isDragging = true
        dragAnchorIndex = continuousPage.roundToInt().coerceIn(0, (tabRoutes.size - 1).coerceAtLeast(0))
    }

    internal fun dragByPixels(dxPx: Float, widthPx: Float, dirSign: Float) {
        if (widthPx <= 1f || tabRoutes.isEmpty()) return
        val lastIdx = (tabRoutes.size - 1).coerceAtLeast(0).toFloat()
        val rawDeltaPages = (-dxPx * dirSign) / widthPx
        val candidate = continuousPage + rawDeltaPages
        val damped = when {
            candidate < 0f && rawDeltaPages < 0f -> continuousPage + rawDeltaPages * 0.20f
            candidate > lastIdx && rawDeltaPages > 0f -> continuousPage + rawDeltaPages * 0.20f
            else -> candidate
        }
        val minAllowed = (dragAnchorIndex - 1f).coerceAtLeast(-0.22f)
        val maxAllowed = (dragAnchorIndex + 1f).coerceAtMost(lastIdx + 0.22f)
        continuousPage = damped.coerceIn(minAllowed, maxAllowed)
    }

    internal fun endDragAndSettle(
        scope: CoroutineScope,
        velocityXPxPerSec: Float,
        widthPx: Float,
        dirSign: Float
    ) {
        isDragging = false
        if (tabRoutes.isEmpty()) return
        val lastIdx = (tabRoutes.size - 1).coerceAtLeast(0)
        val vPagesPerSec = if (widthPx > 1f) (-velocityXPxPerSec * dirSign) / widthPx else 0f
        val deltaFromAnchor = continuousPage - dragAnchorIndex.toFloat()

        val targetIdx = when {
            vPagesPerSec >= 0.55f && deltaFromAnchor > -0.15f ->
                (dragAnchorIndex + 1).coerceAtMost(lastIdx)
            vPagesPerSec <= -0.55f && deltaFromAnchor < 0.15f ->
                (dragAnchorIndex - 1).coerceAtLeast(0)
            deltaFromAnchor >= 0.18f ->
                (dragAnchorIndex + 1).coerceAtMost(lastIdx)
            deltaFromAnchor <= -0.18f ->
                (dragAnchorIndex - 1).coerceAtLeast(0)
            else -> dragAnchorIndex
        }.coerceIn(0, lastIdx)

        activeIndex = targetIdx
        activeRoute = tabRoutes[targetIdx]
        dragAnchorIndex = targetIdx

        val token = ++animToken
        isSettling = true
        animationJob = scope.launch {
            try {
                val anim = Animatable(continuousPage)
                anim.animateTo(
                    targetValue = targetIdx.toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 540f
                    ),
                    initialVelocity = vPagesPerSec.coerceIn(-14f, 14f)
                ) {
                    continuousPage = value
                }
                continuousPage = targetIdx.toFloat()
            } finally {
                if (animToken == token) {
                    isSettling = false
                }
            }
        }
    }

    private fun clearJumpMode() {
        jumpFromIndex = -1
        jumpToIndex = -1
        jumpProgress = 0f
    }
}

@Composable
fun rememberSmoothTabSwipeDeckState(
    tabs: List<String>,
    initialRoute: String
): SmoothTabSwipeDeckState {
    val state = remember {
        SmoothTabSwipeDeckState(
            initialTabs = tabs,
            initialRoute = initialRoute
        )
    }
    LaunchedEffect(tabs) {
        state.syncTabsOrder(tabs)
    }
    return state
}

/**
 * Pre-warmed GPU RenderNode deck for instant, zero-jank horizontal tab swiping.
 *
 * - Frame 1 composes only the active tab for fast cold launch; Frame 2 pre-warms all tabs
 *   so subsequent swipes never run cold subcomposition or recreate `LazyColumn` scroll states.
 * - Swiping mutates only `continuousPage`, which is read strictly inside `Modifier.graphicsLayer`,
 *   yielding 0 recompositions, 0 measure/layout passes, and 0 offscreen `saveLayer` buffers.
 * - Full RTL (Arabic) and LTR (English) directional symmetry.
 */
@Composable
fun SmoothTabSwipeDeck(
    state: SmoothTabSwipeDeckState,
    modifier: Modifier = Modifier,
    pageContent: @Composable (route: String, isActiveTab: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val dirSign = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
    val currentDirSign by rememberUpdatedState(dirSign)

    val containerWidthPx = remember { mutableFloatStateOf(0f) }
    var isPrewarmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        isPrewarmed = true
    }

    val routes = state.tabs
    val activeIdx = state.activeIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { sz ->
                containerWidthPx.floatValue = sz.width.toFloat()
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Main
                    )
                    if (down.isConsumed) return@awaitEachGesture

                    val widthPx = containerWidthPx.floatValue.takeIf { it > 1f } ?: size.width.toFloat()
                    if (widthPx <= 1f || state.tabs.size <= 1) return@awaitEachGesture

                    val pointerId = down.id
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    val touchSlop = viewConfiguration.touchSlop
                    var totalDx = 0f
                    var totalDy = 0f
                    var isHorizontalLocked = false

                    // Catch mid-flight spring settle seamlessly if noticeably between pages
                    if (
                        state.isSettling &&
                        !state.isJumpAnimating &&
                        abs(state.continuousPage - state.continuousPage.roundToInt().toFloat()) > 0.03f
                    ) {
                        state.beginDrag()
                        isHorizontalLocked = true
                    }

                    while (true) {
                        val eventPass = if (isHorizontalLocked) {
                            PointerEventPass.Initial
                        } else {
                            PointerEventPass.Main
                        }
                        val event = awaitPointerEvent(eventPass)
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                        if (!change.pressed) {
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            if (isHorizontalLocked) {
                                change.consume()
                            }
                            break
                        }

                        val stepDelta = change.positionChange()
                        velocityTracker.addPosition(change.uptimeMillis, change.position)

                        if (!isHorizontalLocked) {
                            if (change.isConsumed) {
                                return@awaitEachGesture
                            }
                            totalDx += stepDelta.x
                            totalDy += stepDelta.y
                            val absDx = abs(totalDx)
                            val absDy = abs(totalDy)

                            if (absDy > touchSlop * 0.85f && absDy > absDx * 0.95f) {
                                return@awaitEachGesture
                            }

                            if (absDx > touchSlop * 0.65f && absDx > absDy * 1.15f) {
                                isHorizontalLocked = true
                                state.beginDrag()
                                val postSlopDx = totalDx - (touchSlop * 0.65f * sign(totalDx))
                                change.consume()
                                state.dragByPixels(
                                    dxPx = postSlopDx,
                                    widthPx = widthPx,
                                    dirSign = currentDirSign
                                )
                            }
                        } else {
                            change.consume()
                            if (stepDelta.x != 0f) {
                                state.dragByPixels(
                                    dxPx = stepDelta.x,
                                    widthPx = widthPx,
                                    dirSign = currentDirSign
                                )
                            }
                        }
                    }

                    if (isHorizontalLocked) {
                        val vx = velocityTracker.calculateVelocity().x
                        state.endDragAndSettle(
                            scope = scope,
                            velocityXPxPerSec = vx,
                            widthPx = widthPx,
                            dirSign = currentDirSign
                        )
                    }
                }
            }
    ) {
        routes.forEachIndexed { index, route ->
            val shouldCompose = isPrewarmed || index == activeIdx
            if (shouldCompose) {
                key(route) {
                    val isActive = index == activeIdx
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (isActive) 2f else 1f)
                            .graphicsLayer {
                                val w = containerWidthPx.floatValue
                                    .takeIf { it > 1f }
                                    ?: size.width.takeIf { it > 1f }
                                    ?: 10000f

                                val jFrom = state.jumpFromIndex
                                val jTo = state.jumpToIndex
                                if (jFrom >= 0 && jTo >= 0 && jFrom != jTo) {
                                    val forwardSign = if (jTo > jFrom) 1f else -1f
                                    val p = state.jumpProgress
                                    when (index) {
                                        jFrom -> {
                                            alpha = 1f
                                            translationX = (-p * forwardSign) * w * dirSign
                                        }
                                        jTo -> {
                                            alpha = 1f
                                            translationX = ((1f - p) * forwardSign) * w * dirSign
                                        }
                                        else -> {
                                            alpha = 0f
                                            translationX = w * 2.5f
                                        }
                                    }
                                } else {
                                    val rel = index.toFloat() - state.continuousPage
                                    if (abs(rel) < 1.05f) {
                                        alpha = 1f
                                        translationX = rel * w * dirSign
                                    } else {
                                        alpha = 0f
                                        translationX = w * 2.5f
                                    }
                                }
                            }
                            .then(
                                if (!isActive) {
                                    Modifier.clearAndSetSemantics { }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        pageContent(route, isActive)
                    }
                }
            }
        }
    }
}
