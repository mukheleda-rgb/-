package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.CloudTrack
import com.example.data.scanner.TrackSortOption
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.LocalNaghamDesign
import kotlinx.coroutines.launch

@Immutable
data class CategoryFilterItem(
    val id: String,
    val label: String
)

/**
 * Single-row primary options toolbar where sub-options for Select, Folders, Sort, and Category
 * remain hidden until their button is clicked, opening a vertical theme-colored DropdownMenu
 * anchored right at that button's exact position.
 */
@Composable
fun TrackSortBar(
    selectedSort: TrackSortOption,
    onSelectSort: (TrackSortOption) -> Unit,
    scopeTagPrefix: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isSelectionMode: Boolean = false,
    onToggleSelectionMode: (() -> Unit)? = null,
    onSelectAllRequest: (() -> Unit)? = null,
    showFoldersMenu: Boolean = false,
    availableFolders: List<String> = emptyList(),
    selectedFolder: String = "ALL_FOLDERS",
    onSelectFolder: (String) -> Unit = {},
    onHideFolder: ((String) -> Unit)? = null,
    onManageExcludedFoldersClick: (() -> Unit)? = null,
    categoryOptions: List<CategoryFilterItem> = emptyList(),
    selectedCategoryId: String = "ALL",
    onSelectCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val designTokens = LocalNaghamDesign.current

    var selectMenuExpanded by remember { mutableStateOf(false) }
    var foldersMenuExpanded by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val sortShortLabel = when (selectedSort) {
        TrackSortOption.NAME -> strings.sortByName
        TrackSortOption.DATE -> strings.sortByDate
        TrackSortOption.DURATION -> strings.sortByDuration
        TrackSortOption.CUSTOM -> strings.sortByCustom
    }

    val activeFolderLabel = if (selectedFolder == "ALL_FOLDERS") {
        strings.foldersLabel
    } else {
        selectedFolder
    }

    val activeCategoryLabel = remember(categoryOptions, selectedCategoryId, strings) {
        if (selectedCategoryId == "ALL") {
            strings.filterCategoryBtn
        } else {
            categoryOptions.firstOrNull { it.id == selectedCategoryId }?.label ?: strings.filterCategoryBtn
        }
    }

    val menuContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val menuBorder = BorderStroke(1.dp, accentColor.copy(alpha = 0.48f))
    val menuItemColors = MenuDefaults.itemColors(
        textColor = MaterialTheme.colorScheme.onSurface,
        leadingIconColor = accentColor,
        trailingIconColor = accentColor
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${scopeTagPrefix}_sort_bar"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val barScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(barScrollState, enabled = barScrollState.maxValue > 0),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Select Primary Button + Anchored Vertical Menu
            if (onToggleSelectionMode != null) {
                Box {
                    ToolbarMenuPillButton(
                        label = strings.selectModeBtn,
                        icon = Icons.Default.Checklist,
                        isActive = isSelectionMode || selectMenuExpanded,
                        isMenuOpen = selectMenuExpanded,
                        accentColor = accentColor,
                        onClick = { selectMenuExpanded = !selectMenuExpanded },
                        testTag = "${scopeTagPrefix}_select_menu_button"
                    )

                    DropdownMenu(
                        expanded = selectMenuExpanded,
                        onDismissRequest = { selectMenuExpanded = false },
                        shape = designTokens.cardShape,
                        containerColor = menuContainerColor,
                        border = menuBorder,
                        modifier = Modifier.widthIn(min = 190.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isSelectionMode) strings.clearSelectionBtn else strings.actionSelectMultiple,
                                    fontWeight = if (isSelectionMode) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Checklist,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (isSelectionMode) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = menuItemColors,
                            onClick = {
                                selectMenuExpanded = false
                                onToggleSelectionMode()
                            },
                            modifier = Modifier.testTag("${scopeTagPrefix}_select_mode_button")
                        )

                        if (onSelectAllRequest != null) {
                            DropdownMenuItem(
                                text = { Text(strings.selectAllBtn) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = menuItemColors,
                                onClick = {
                                    selectMenuExpanded = false
                                    if (!isSelectionMode) {
                                        onToggleSelectionMode()
                                    }
                                    onSelectAllRequest()
                                },
                                modifier = Modifier.testTag("${scopeTagPrefix}_menu_select_all")
                            )
                        }
                    }
                }
            }

            // 2. Folders Primary Button + Anchored Vertical Menu (with one-tap Hide & Manage Excluded)
            if (showFoldersMenu) {
                Box {
                    ToolbarMenuPillButton(
                        label = activeFolderLabel,
                        icon = Icons.Default.Folder,
                        isActive = selectedFolder != "ALL_FOLDERS" || foldersMenuExpanded,
                        isMenuOpen = foldersMenuExpanded,
                        accentColor = accentColor,
                        onClick = { foldersMenuExpanded = !foldersMenuExpanded },
                        testTag = "${scopeTagPrefix}_folders_menu_button"
                    )

                    DropdownMenu(
                        expanded = foldersMenuExpanded,
                        onDismissRequest = { foldersMenuExpanded = false },
                        shape = designTokens.cardShape,
                        containerColor = menuContainerColor,
                        border = menuBorder,
                        modifier = Modifier.widthIn(min = 230.dp, max = 300.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = strings.allFoldersChip,
                                    fontWeight = if (selectedFolder == "ALL_FOLDERS") FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (selectedFolder == "ALL_FOLDERS") {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = menuItemColors,
                            onClick = {
                                foldersMenuExpanded = false
                                onSelectFolder("ALL_FOLDERS")
                            },
                            modifier = Modifier.testTag("${scopeTagPrefix}_folder_ALL")
                        )

                        availableFolders.forEach { folder ->
                            val isSelectedFolder = selectedFolder == folder
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = folder,
                                        fontWeight = if (isSelectedFolder) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = if (onHideFolder != null) {
                                    {
                                        IconButton(
                                            onClick = {
                                                foldersMenuExpanded = false
                                                if (selectedFolder == folder) {
                                                    onSelectFolder("ALL_FOLDERS")
                                                }
                                                onHideFolder(folder)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .testTag("${scopeTagPrefix}_hide_folder_$folder")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VisibilityOff,
                                                contentDescription = strings.hideFolderAction,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                } else null,
                                colors = menuItemColors,
                                onClick = {
                                    foldersMenuExpanded = false
                                    onSelectFolder(folder)
                                },
                                modifier = Modifier.testTag("${scopeTagPrefix}_folder_item_$folder")
                            )
                        }

                        if (onManageExcludedFoldersClick != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strings.manageExcludedFoldersBtn,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = accentColor
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOff,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = menuItemColors,
                                onClick = {
                                    foldersMenuExpanded = false
                                    onManageExcludedFoldersClick()
                                },
                                modifier = Modifier.testTag("${scopeTagPrefix}_manage_excluded_folders_button")
                            )
                        }
                    }
                }
            }

            // 3. Sort Primary Button + Anchored Vertical Menu
            Box {
                ToolbarMenuPillButton(
                    label = sortShortLabel,
                    icon = Icons.AutoMirrored.Filled.Sort,
                    isActive = sortMenuExpanded || selectedSort != TrackSortOption.NAME,
                    isMenuOpen = sortMenuExpanded,
                    accentColor = accentColor,
                    onClick = { sortMenuExpanded = !sortMenuExpanded },
                    testTag = "${scopeTagPrefix}_sort_menu_button"
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    shape = designTokens.cardShape,
                    containerColor = menuContainerColor,
                    border = menuBorder,
                    modifier = Modifier.widthIn(min = 195.dp)
                ) {
                    SortDropdownItem(
                        label = strings.sortByName,
                        icon = Icons.Default.SortByAlpha,
                        selected = selectedSort == TrackSortOption.NAME,
                        accentColor = accentColor,
                        testTag = "${scopeTagPrefix}_sort_${TrackSortOption.NAME.id}",
                        onClick = {
                            sortMenuExpanded = false
                            onSelectSort(TrackSortOption.NAME)
                        }
                    )
                    SortDropdownItem(
                        label = strings.sortByDate,
                        icon = Icons.Default.Schedule,
                        selected = selectedSort == TrackSortOption.DATE,
                        accentColor = accentColor,
                        testTag = "${scopeTagPrefix}_sort_${TrackSortOption.DATE.id}",
                        onClick = {
                            sortMenuExpanded = false
                            onSelectSort(TrackSortOption.DATE)
                        }
                    )
                    SortDropdownItem(
                        label = strings.sortByDuration,
                        icon = Icons.Default.AccessTime,
                        selected = selectedSort == TrackSortOption.DURATION,
                        accentColor = accentColor,
                        testTag = "${scopeTagPrefix}_sort_${TrackSortOption.DURATION.id}",
                        onClick = {
                            sortMenuExpanded = false
                            onSelectSort(TrackSortOption.DURATION)
                        }
                    )
                    SortDropdownItem(
                        label = strings.sortByCustom,
                        icon = Icons.Default.DragHandle,
                        selected = selectedSort == TrackSortOption.CUSTOM,
                        accentColor = accentColor,
                        testTag = "${scopeTagPrefix}_sort_${TrackSortOption.CUSTOM.id}",
                        onClick = {
                            sortMenuExpanded = false
                            onSelectSort(TrackSortOption.CUSTOM)
                        }
                    )
                }
            }

            // 4. Category Primary Button + Anchored Vertical Menu
            if (categoryOptions.isNotEmpty()) {
                Box {
                    ToolbarMenuPillButton(
                        label = activeCategoryLabel,
                        icon = Icons.Default.Category,
                        isActive = selectedCategoryId != "ALL" || categoryMenuExpanded,
                        isMenuOpen = categoryMenuExpanded,
                        accentColor = accentColor,
                        onClick = { categoryMenuExpanded = !categoryMenuExpanded },
                        testTag = "${scopeTagPrefix}_category_menu_button"
                    )

                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        shape = designTokens.cardShape,
                        containerColor = menuContainerColor,
                        border = menuBorder,
                        modifier = Modifier.widthIn(min = 195.dp)
                    ) {
                        categoryOptions.forEach { catItem ->
                            val isSelectedCat = selectedCategoryId == catItem.id
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = catItem.label,
                                        fontWeight = if (isSelectedCat) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelectedCat) accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                trailingIcon = if (isSelectedCat) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = menuItemColors,
                                onClick = {
                                    categoryMenuExpanded = false
                                    onSelectCategory(catItem.id)
                                },
                                modifier = Modifier.testTag("${scopeTagPrefix}_category_${catItem.id}")
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedSort == TrackSortOption.CUSTOM,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = strings.dragHandleHint,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ToolbarMenuPillButton(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    isMenuOpen: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val designTokens = LocalNaghamDesign.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isMenuOpen) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "toolbar_chevron_rotation"
    )

    Surface(
        shape = designTokens.pillShape,
        color = if (isActive) {
            accentColor.copy(alpha = 0.22f)
        } else {
            designTokens.cardContainerColor(MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        border = BorderStroke(
            width = if (isActive) 1.4.dp else 1.dp,
            color = if (isActive) {
                accentColor.copy(alpha = 0.82f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = 40.dp)
            .clip(designTokens.pillShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null,
                tint = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = chevronRotation }
            )
        }
    }
}

@Composable
private fun SortDropdownItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        onClick = onClick,
        modifier = Modifier.testTag(testTag)
    )
}

@Stable
class ReorderableTrackListController(
    val lazyListState: LazyListState
) {
    internal val handleCoords = LinkedHashMap<String, LayoutCoordinates>()
    internal var containerCoords: LayoutCoordinates? = null

    var draggedTrackId by mutableStateOf<String?>(null)
        private set

    var initialViewportOffsetPx by mutableIntStateOf(0)
        private set

    var draggedItemHeightPx by mutableIntStateOf(0)
        private set

    var initialTouchYPx by mutableFloatStateOf(0f)
        private set

    var currentTouchYPx by mutableFloatStateOf(0f)
        private set

    val desiredViewportTopPx: Float
        get() = initialViewportOffsetPx.toFloat() + (currentTouchYPx - initialTouchYPx)

    internal fun registerHandle(trackId: String, coords: LayoutCoordinates) {
        handleCoords[trackId] = coords
    }

    internal fun unregisterHandle(trackId: String) {
        handleCoords.remove(trackId)
    }

    internal fun startDrag(
        trackId: String,
        initialOffsetPx: Int,
        itemHeightPx: Int,
        touchYPx: Float
    ) {
        draggedTrackId = trackId
        initialViewportOffsetPx = initialOffsetPx
        draggedItemHeightPx = itemHeightPx
        initialTouchYPx = touchYPx
        currentTouchYPx = touchYPx
    }

    internal fun updateTouchY(touchYPx: Float) {
        currentTouchYPx = touchYPx
    }

    internal fun endDrag() {
        draggedTrackId = null
        initialViewportOffsetPx = 0
        draggedItemHeightPx = 0
        initialTouchYPx = 0f
        currentTouchYPx = 0f
    }
}

@Composable
fun rememberReorderableTrackListController(
    lazyListState: LazyListState
): ReorderableTrackListController {
    return remember(lazyListState) { ReorderableTrackListController(lazyListState) }
}

/**
 * Wraps a LazyColumn so touches starting inside a [TrackDragHandle] immediately enter smooth,
 * floating drag-to-reorder mode, while touches anywhere else pass straight through untouched
 * (preserving normal scrolling, tap-to-play, and long-press actions).
 */
@Composable
fun ReorderableTrackListBox(
    controller: ReorderableTrackListController,
    tracks: List<CloudTrack>,
    onMoveTrack: (movedTrackId: String, targetTrackId: String) -> Unit,
    onDragFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentTracks by rememberUpdatedState(tracks)
    val currentOnMove by rememberUpdatedState(onMoveTrack)
    val currentOnDragFinished by rememberUpdatedState(onDragFinished)

    val edgeZonePx = with(density) { 56.dp.toPx() }
    val autoScrollStepPx = with(density) { 14.dp.toPx() }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                controller.containerCoords = coords
            }
            .pointerInput(controller) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    val boxCoords = controller.containerCoords
                    if (boxCoords == null || !boxCoords.isAttached) {
                        return@awaitEachGesture
                    }

                    // Check if pointer down landed inside any TrackDragHandle
                    val hitTrackId = controller.handleCoords.entries.firstOrNull { (_, hCoords) ->
                        if (!hCoords.isAttached) return@firstOrNull false
                        val rect = try {
                            boxCoords.localBoundingBoxOf(hCoords, clipBounds = false).inflate(6f)
                        } catch (_: Throwable) {
                            return@firstOrNull false
                        }
                        rect.contains(down.position)
                    }?.key ?: return@awaitEachGesture

                    val initialItemInfo = controller.lazyListState.layoutInfo.visibleItemsInfo
                        .firstOrNull { it.key == hitTrackId }
                        ?: return@awaitEachGesture

                    down.consume()
                    controller.startDrag(
                        trackId = hitTrackId,
                        initialOffsetPx = initialItemInfo.offset,
                        itemHeightPx = initialItemInfo.size.coerceAtLeast(1),
                        touchYPx = down.position.y
                    )
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                    val workingTrackIds = currentTracks.map { it.id }.toMutableList()
                    var movedAtLeastOnce = false
                    try {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            controller.updateTouchY(change.position.y)

                            // Re-sync any newly loaded IDs if list size changed
                            val snapshot = currentTracks
                            if (snapshot.size != workingTrackIds.size) {
                                workingTrackIds.clear()
                                snapshot.forEach { workingTrackIds.add(it.id) }
                            }

                            val indexById = HashMap<String, Int>(workingTrackIds.size)
                            workingTrackIds.forEachIndexed { i, id -> indexById[id] = i }

                            val fromIdx = indexById[hitTrackId]
                            if (fromIdx != null) {
                                val desiredCenterY =
                                    controller.desiredViewportTopPx + controller.draggedItemHeightPx * 0.5f

                                val targetInfo = controller.lazyListState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { info ->
                                        val otherId = info.key as? String ?: return@firstOrNull false
                                        if (otherId == hitTrackId) return@firstOrNull false
                                        val toIdx = indexById[otherId] ?: return@firstOrNull false
                                        if (toIdx > fromIdx) {
                                            desiredCenterY > info.offset + info.size * 0.45f
                                        } else {
                                            desiredCenterY < info.offset + info.size * 0.55f
                                        }
                                    }

                                val targetId = targetInfo?.key as? String
                                val toIdx = if (targetId != null) indexById[targetId] else null
                                if (targetId != null && toIdx != null && toIdx != fromIdx) {
                                    val moved = workingTrackIds.removeAt(fromIdx)
                                    workingTrackIds.add(toIdx, moved)
                                    movedAtLeastOnce = true
                                    currentOnMove(hitTrackId, targetId)
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }

                            // Gentle auto-scroll near top or bottom of LazyColumn viewport
                            val viewportHeight = size.height.toFloat()
                            if (viewportHeight > edgeZonePx * 2f) {
                                when {
                                    change.position.y < edgeZonePx -> {
                                        scope.launch {
                                            controller.lazyListState.scrollBy(-autoScrollStepPx)
                                        }
                                    }
                                    change.position.y > viewportHeight - edgeZonePx -> {
                                        scope.launch {
                                            controller.lazyListState.scrollBy(autoScrollStepPx)
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        controller.endDrag()
                        if (movedAtLeastOnce) {
                            currentOnDragFinished()
                        }
                    }
                }
            },
        content = content
    )
}

/**
 * Dedicated tactile drag handle placed on each track item so users can freely drag & reorder
 * tracks without conflicting with list scrolling or card tap/long-press gestures.
 */
@Composable
fun TrackDragHandle(
    trackId: String,
    controller: ReorderableTrackListController?,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    if (controller == null) return
    val strings = LocalAppStrings.current
    val isActive = controller.draggedTrackId == trackId

    DisposableEffect(controller, trackId) {
        onDispose {
            controller.unregisterHandle(trackId)
        }
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                }
            )
            .onGloballyPositioned { coords ->
                controller.registerHandle(trackId, coords)
            }
            .semantics(mergeDescendants = true) {}
            .testTag("drag_handle_$trackId"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = strings.dragToReorderDesc,
            tint = if (isActive) MaterialTheme.colorScheme.primary else tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun Modifier.reorderableTrackItem(
    trackId: String,
    controller: ReorderableTrackListController?
): Modifier {
    if (controller == null) return this
    val isDragging = controller.draggedTrackId == trackId
    val density = LocalDensity.current
    val shadowPx = with(density) { 12.dp.toPx() }
    val primaryColor = MaterialTheme.colorScheme.primary

    return this
        .zIndex(if (isDragging) 25f else 0f)
        .graphicsLayer {
            if (isDragging) {
                val itemInfo = controller.lazyListState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == trackId }
                if (itemInfo != null) {
                    translationY = controller.desiredViewportTopPx - itemInfo.offset.toFloat()
                }
                scaleX = 1.025f
                scaleY = 1.025f
                shadowElevation = shadowPx
                shape = RoundedCornerShape(20.dp)
            } else {
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                shadowElevation = 0f
            }
        }
        .then(
            if (isDragging) {
                Modifier.border(1.5.dp, primaryColor, RoundedCornerShape(20.dp))
            } else {
                Modifier
            }
        )
}
