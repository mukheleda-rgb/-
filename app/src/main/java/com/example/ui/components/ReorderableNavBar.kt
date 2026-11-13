package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.naghamDesignBorder
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

data class DraggableNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private data class AxisSlotSpan(
    val startPx: Float,
    val sizePx: Float
) {
    val endPx: Float get() = startPx + sizePx
    val centerPx: Float get() = startPx + sizePx * 0.5f
}

@Composable
fun ReorderableBottomNavigationBar(
    tabs: List<DraggableNavTab>,
    selectedRoute: String,
    onSelectTab: (Int, DraggableNavTab) -> Unit,
    onMoveTab: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    swipePositionProvider: (() -> Float)? = null
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val designTokens = LocalNaghamDesign.current
    val currentTabs by rememberUpdatedState(tabs)
    val currentOnMove by rememberUpdatedState(onMoveTab)

    val slotSpansByIndex = remember { mutableStateMapOf<Int, AxisSlotSpan>() }

    var draggedRoute by remember { mutableStateOf<String?>(null) }
    var fingerPosPx by remember { mutableFloatStateOf(0f) }
    var grabOffsetWithinTabPx by remember { mutableFloatStateOf(0f) }

    val floatingLiftPx = with(density) { 8.dp.toPx() }
    val floatingShadowPx = with(density) { 10.dp.toPx() }
    val itemPillShape = designTokens.pillShape

    val hasMeasuredSlots = tabs.isNotEmpty() && slotSpansByIndex.size >= tabs.size
    val useLiveSlidingPill = swipePositionProvider != null && draggedRoute == null && hasMeasuredSlots

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (designTokens.isFloatingNavBar) {
                    Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                } else {
                    Modifier
                }
            )
    ) {
        Surface(
            shape = designTokens.navBarShape,
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = designTokens.navBarAlpha),
            tonalElevation = if (designTokens.isFloatingNavBar) 0.dp else 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .naghamDesignBorder(
                    accentColor = MaterialTheme.colorScheme.primary,
                    shapeOverride = designTokens.navBarShape
                )
                .testTag("reorderable_bottom_nav")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!designTokens.isFloatingNavBar) {
                            Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                if (useLiveSlidingPill) {
                    val referenceSlotWidthPx = (slotSpansByIndex[0]?.sizePx ?: 1f).coerceAtLeast(1f)
                    val pillWidthDp = with(density) { referenceSlotWidthPx.toDp() }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier.matchParentSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(pillWidthDp)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        val maxIdx = (currentTabs.size - 1).coerceAtLeast(0)
                                        val rawPos = (swipePositionProvider?.invoke() ?: 0f)
                                            .coerceIn(0f, maxIdx.toFloat())
                                        val lowIdx = floor(rawPos).toInt().coerceIn(0, maxIdx)
                                        val highIdx = ceil(rawPos).toInt().coerceIn(0, maxIdx)
                                        val frac = (rawPos - lowIdx.toFloat()).coerceIn(0f, 1f)
                                        val lowSpan = slotSpansByIndex[lowIdx]
                                        val highSpan = slotSpansByIndex[highIdx]
                                        if (lowSpan != null && highSpan != null) {
                                            val interpStart = lowSpan.startPx +
                                                (highSpan.startPx - lowSpan.startPx) * frac
                                            val interpWidth = lowSpan.sizePx +
                                                (highSpan.sizePx - lowSpan.sizePx) * frac
                                            translationX = interpStart
                                            scaleX = (interpWidth / referenceSlotWidthPx).coerceIn(0.5f, 2f)
                                        }
                                    }
                                    .clip(itemPillShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        itemPillShape
                                    )
                                    .testTag("nav_sliding_active_pill")
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            var workingRoutes = mutableListOf<String>()

                            detectDragGesturesAfterLongPress(
                                onDragStart = { startOffset ->
                                    val tabsSnapshot = currentTabs
                                    if (tabsSnapshot.isEmpty()) return@detectDragGesturesAfterLongPress

                                    val hitIdx = tabsSnapshot.indices.firstOrNull { idx ->
                                        val span = slotSpansByIndex[idx] ?: return@firstOrNull false
                                        startOffset.x >= span.startPx - 4f && startOffset.x <= span.endPx + 4f
                                    } ?: tabsSnapshot.indices.minByOrNull { idx ->
                                        val span = slotSpansByIndex[idx]
                                        if (span != null) abs(startOffset.x - span.centerPx) else Float.MAX_VALUE
                                    } ?: return@detectDragGesturesAfterLongPress

                                    val hitSpan = slotSpansByIndex[hitIdx] ?: return@detectDragGesturesAfterLongPress
                                    workingRoutes = tabsSnapshot.map { it.route }.toMutableList()
                                    draggedRoute = workingRoutes[hitIdx]
                                    grabOffsetWithinTabPx = (startOffset.x - hitSpan.startPx)
                                        .coerceIn(0f, hitSpan.sizePx.coerceAtLeast(1f))
                                    fingerPosPx = startOffset.x
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragEnd = {
                                    workingRoutes.clear()
                                    draggedRoute = null
                                    fingerPosPx = 0f
                                    grabOffsetWithinTabPx = 0f
                                },
                                onDragCancel = {
                                    workingRoutes.clear()
                                    draggedRoute = null
                                    fingerPosPx = 0f
                                    grabOffsetWithinTabPx = 0f
                                },
                                onDrag = { change, _ ->
                                    val activeRoute = draggedRoute ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    fingerPosPx = change.position.x

                                    val fromIdx = workingRoutes.indexOf(activeRoute)
                                    if (fromIdx < 0) return@detectDragGesturesAfterLongPress

                                    val activeSpan = slotSpansByIndex[fromIdx] ?: return@detectDragGesturesAfterLongPress
                                    val minStart = workingRoutes.indices
                                        .mapNotNull { slotSpansByIndex[it]?.startPx }
                                        .minOrNull() ?: 0f
                                    val maxEnd = workingRoutes.indices
                                        .mapNotNull { slotSpansByIndex[it]?.endPx }
                                        .maxOrNull() ?: size.width.toFloat()

                                    val desiredStart = (fingerPosPx - grabOffsetWithinTabPx)
                                        .coerceIn(minStart, (maxEnd - activeSpan.sizePx).coerceAtLeast(minStart))
                                    val desiredCenter = desiredStart + activeSpan.sizePx * 0.5f

                                    val targetIdx = workingRoutes.indices.firstOrNull { idx ->
                                        val span = slotSpansByIndex[idx] ?: return@firstOrNull false
                                        val margin = span.sizePx * 0.12f
                                        desiredCenter >= (span.startPx + margin) &&
                                            desiredCenter <= (span.endPx - margin)
                                    }

                                    if (targetIdx != null && targetIdx != fromIdx && targetIdx in workingRoutes.indices) {
                                        val moved = workingRoutes.removeAt(fromIdx)
                                        workingRoutes.add(targetIdx, moved)
                                        currentOnMove(fromIdx, targetIdx)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        key(tab.route) {
                            val latestIndex by rememberUpdatedState(index)
                            val isSelected = tab.route == selectedRoute
                            val isDragging = draggedRoute == tab.route
                            val scale by animateFloatAsState(
                                targetValue = when {
                                    isDragging -> 1.10f
                                    isSelected -> 1.04f
                                    else -> 1f
                                },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "tab_drag_scale"
                            )
                            val pillBgColor by animateColorAsState(
                                targetValue = when {
                                    isDragging -> MaterialTheme.colorScheme.primaryContainer
                                    !useLiveSlidingPill && isSelected ->
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                                    else -> Color.Transparent
                                },
                                label = "tab_pill_bg"
                            )

                            // Outer slot Box measures pure layout coordinates inside Row (untouched by graphicsLayer)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .zIndex(if (isDragging) 20f else 0f)
                                    .onGloballyPositioned { coords ->
                                        if (coords.isAttached) {
                                            val pos = coords.positionInParent()
                                            slotSpansByIndex[latestIndex] = AxisSlotSpan(
                                                startPx = pos.x,
                                                sizePx = coords.size.width.toFloat().coerceAtLeast(1f)
                                            )
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                            if (isDragging) {
                                                val mySlot = slotSpansByIndex[latestIndex]
                                                if (mySlot != null) {
                                                    val minStart = tabs.indices
                                                        .mapNotNull { slotSpansByIndex[it]?.startPx }
                                                        .minOrNull() ?: 0f
                                                    val maxEnd = tabs.indices
                                                        .mapNotNull { slotSpansByIndex[it]?.endPx }
                                                        .maxOrNull() ?: mySlot.endPx
                                                    val desiredStart = (fingerPosPx - grabOffsetWithinTabPx)
                                                        .coerceIn(
                                                            minStart,
                                                            (maxEnd - mySlot.sizePx).coerceAtLeast(minStart)
                                                        )
                                                    translationX = desiredStart - mySlot.startPx
                                                }
                                                translationY = -floatingLiftPx
                                                shadowElevation = floatingShadowPx
                                            } else {
                                                translationX = 0f
                                                translationY = 0f
                                                shadowElevation = 0f
                                            }
                                            shape = itemPillShape
                                        }
                                        .minimumInteractiveComponentSize()
                                        .clip(itemPillShape)
                                        .background(pillBgColor)
                                        .then(
                                            when {
                                                isDragging -> Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    itemPillShape
                                                )
                                                !useLiveSlidingPill && isSelected -> Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                                    itemPillShape
                                                )
                                                else -> Modifier
                                            }
                                        )
                                        .clickable {
                                            if (draggedRoute == null) {
                                                onSelectTab(latestIndex, tab)
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .testTag(tab.testTag),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                            tint = if (isSelected || isDragging) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.size(23.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected || isDragging) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReorderableSideNavigationRail(
    tabs: List<DraggableNavTab>,
    selectedRoute: String,
    onSelectTab: (Int, DraggableNavTab) -> Unit,
    onMoveTab: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val designTokens = LocalNaghamDesign.current
    val currentTabs by rememberUpdatedState(tabs)
    val currentOnMove by rememberUpdatedState(onMoveTab)

    val slotSpansByIndex = remember { mutableStateMapOf<Int, AxisSlotSpan>() }

    var draggedRoute by remember { mutableStateOf<String?>(null) }
    var fingerPosY by remember { mutableFloatStateOf(0f) }
    var grabOffsetWithinTabY by remember { mutableFloatStateOf(0f) }

    val floatingShadowPx = with(density) { 10.dp.toPx() }
    val itemPillShape = designTokens.pillShape

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = designTokens.navBarAlpha),
        tonalElevation = if (designTokens.isFloatingNavBar) 0.dp else 2.dp,
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
            .testTag("reorderable_side_rail")
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .pointerInput(Unit) {
                    var workingRoutes = mutableListOf<String>()

                    detectDragGesturesAfterLongPress(
                        onDragStart = { startOffset ->
                            val tabsSnapshot = currentTabs
                            if (tabsSnapshot.isEmpty()) return@detectDragGesturesAfterLongPress

                            val hitIdx = tabsSnapshot.indices.firstOrNull { idx ->
                                val span = slotSpansByIndex[idx] ?: return@firstOrNull false
                                startOffset.y >= span.startPx - 6f && startOffset.y <= span.endPx + 6f
                            } ?: tabsSnapshot.indices.minByOrNull { idx ->
                                val span = slotSpansByIndex[idx]
                                if (span != null) abs(startOffset.y - span.centerPx) else Float.MAX_VALUE
                            } ?: return@detectDragGesturesAfterLongPress

                            val hitSpan = slotSpansByIndex[hitIdx] ?: return@detectDragGesturesAfterLongPress
                            workingRoutes = tabsSnapshot.map { it.route }.toMutableList()
                            draggedRoute = workingRoutes[hitIdx]
                            grabOffsetWithinTabY = (startOffset.y - hitSpan.startPx)
                                .coerceIn(0f, hitSpan.sizePx.coerceAtLeast(1f))
                            fingerPosY = startOffset.y
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            workingRoutes.clear()
                            draggedRoute = null
                            fingerPosY = 0f
                            grabOffsetWithinTabY = 0f
                        },
                        onDragCancel = {
                            workingRoutes.clear()
                            draggedRoute = null
                            fingerPosY = 0f
                            grabOffsetWithinTabY = 0f
                        },
                        onDrag = { change, _ ->
                            val activeRoute = draggedRoute ?: return@detectDragGesturesAfterLongPress
                            change.consume()
                            fingerPosY = change.position.y

                            val fromIdx = workingRoutes.indexOf(activeRoute)
                            if (fromIdx < 0) return@detectDragGesturesAfterLongPress

                            val activeSpan = slotSpansByIndex[fromIdx] ?: return@detectDragGesturesAfterLongPress
                            val minStart = workingRoutes.indices
                                .mapNotNull { slotSpansByIndex[it]?.startPx }
                                .minOrNull() ?: 0f
                            val maxEnd = workingRoutes.indices
                                .mapNotNull { slotSpansByIndex[it]?.endPx }
                                .maxOrNull() ?: size.height.toFloat()

                            val desiredStart = (fingerPosY - grabOffsetWithinTabY)
                                .coerceIn(minStart, (maxEnd - activeSpan.sizePx).coerceAtLeast(minStart))
                            val desiredCenter = desiredStart + activeSpan.sizePx * 0.5f

                            val targetIdx = workingRoutes.indices.firstOrNull { idx ->
                                val span = slotSpansByIndex[idx] ?: return@firstOrNull false
                                val margin = span.sizePx * 0.12f
                                desiredCenter >= (span.startPx + margin) &&
                                    desiredCenter <= (span.endPx - margin)
                            }

                            if (targetIdx != null && targetIdx != fromIdx && targetIdx in workingRoutes.indices) {
                                val moved = workingRoutes.removeAt(fromIdx)
                                workingRoutes.add(targetIdx, moved)
                                currentOnMove(fromIdx, targetIdx)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    )
                },
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tabs.forEachIndexed { index, tab ->
                key(tab.route) {
                    val latestIndex by rememberUpdatedState(index)
                    val isSelected = tab.route == selectedRoute
                    val isDragging = draggedRoute == tab.route
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.08f else 1f,
                        label = "rail_drag_scale"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragging) 20f else 0f)
                            .onGloballyPositioned { coords ->
                                if (coords.isAttached) {
                                    val pos = coords.positionInParent()
                                    slotSpansByIndex[latestIndex] = AxisSlotSpan(
                                        startPx = pos.y,
                                        sizePx = coords.size.height.toFloat().coerceAtLeast(1f)
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    if (isDragging) {
                                        val mySlot = slotSpansByIndex[latestIndex]
                                        if (mySlot != null) {
                                            val minStart = tabs.indices
                                                .mapNotNull { slotSpansByIndex[it]?.startPx }
                                                .minOrNull() ?: 0f
                                            val maxEnd = tabs.indices
                                                .mapNotNull { slotSpansByIndex[it]?.endPx }
                                                .maxOrNull() ?: mySlot.endPx
                                            val desiredStart = (fingerPosY - grabOffsetWithinTabY)
                                                .coerceIn(
                                                    minStart,
                                                    (maxEnd - mySlot.sizePx).coerceAtLeast(minStart)
                                                )
                                            translationY = desiredStart - mySlot.startPx
                                        }
                                        shadowElevation = floatingShadowPx
                                    } else {
                                        translationY = 0f
                                        shadowElevation = 0f
                                    }
                                    shape = itemPillShape
                                }
                                .minimumInteractiveComponentSize()
                                .clip(itemPillShape)
                                .background(
                                    when {
                                        isDragging -> MaterialTheme.colorScheme.primaryContainer
                                        isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                                        else -> Color.Transparent
                                    }
                                )
                                .then(
                                    if (isDragging) {
                                        Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            itemPillShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable {
                                    if (draggedRoute == null) {
                                        onSelectTab(latestIndex, tab)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag(tab.testTag),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected || isDragging) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
