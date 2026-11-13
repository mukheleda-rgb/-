package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserSettings
import com.example.data.scanner.FolderFilterStore
import com.example.ui.components.LanguageModeSwitcher
import com.example.ui.components.ManageExcludedFoldersDialog
import com.example.ui.components.ThemeModeSwitcher
import com.example.ui.localization.LocalAppStrings
import com.example.ui.theme.AppColorPalette
import com.example.ui.theme.AppDesignStyle
import com.example.ui.theme.LocalNaghamColors
import com.example.ui.theme.LocalNaghamDesign
import com.example.ui.theme.PlayerThemeStyle
import com.example.ui.theme.PlayerVisualEffect
import com.example.ui.theme.ThemePreferences
import com.example.ui.theme.buildDesignShapes
import com.example.ui.theme.naghamDesignBorder

@OptIn(ExperimentalLayoutApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsScreen(
    settings: UserSettings,
    selectedPalette: AppColorPalette,
    selectedDesignStyle: AppDesignStyle = AppDesignStyle.MATERIAL,
    selectedPlayerThemeStyle: PlayerThemeStyle = PlayerThemeStyle.AURORA_RING,
    selectedVisualEffect: PlayerVisualEffect = PlayerVisualEffect.VINYL_AURA,
    isScanningStorage: Boolean,
    showGrantAudioPermissionButton: Boolean = false,
    onRequestAudioPermission: () -> Unit = {},
    onDarkThemeChange: (Boolean) -> Unit,
    onToggleEyeComfort: () -> Unit = {},
    onSelectPalette: (AppColorPalette) -> Unit,
    onSelectDesignStyle: (AppDesignStyle) -> Unit = {},
    onSelectPlayerThemeStyle: (PlayerThemeStyle) -> Unit = {},
    onSelectVisualEffect: (PlayerVisualEffect) -> Unit = {},
    onLanguageChange: (String) -> Unit,
    onToggleAutoIsolateQuran: () -> Unit,
    onResetTabOrder: () -> Unit,
    onSignOut: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onBack: () -> Unit = {},
    isActiveTab: Boolean = true
) {
    BackHandler(enabled = isActiveTab, onBack = onBack)
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val extColors = LocalNaghamColors.current
    val designTokens = LocalNaghamDesign.current
    var showExcludedFoldersDialog by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        FolderFilterStore.ensureLoaded(context)
        ThemePreferences.getPauseOnHeadsetDisconnect(context)
    }
    val autoExcludeUnwanted by FolderFilterStore.autoExcludeUnwanted.collectAsStateWithLifecycle()
    val pauseOnHeadsetDisconnect by ThemePreferences.pauseOnHeadsetDisconnectFlow.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
            Card(
                shape = designTokens.heroShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        base = MaterialTheme.colorScheme.surfaceContainerHigh,
                        isHighlighted = true
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder(
                        shapeOverride = designTokens.heroShape,
                        isHighlighted = true
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.settingsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 1. Appearance: Light / Dark ONLY (Night Eye-Comfort filter removed per user request)
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.NightlightRound,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsAppearanceHeader,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    ThemeModeSwitcher(
                        darkTheme = settings.darkTheme,
                        onDarkThemeChange = onDarkThemeChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Player Themes with Miniature Preview Cards (5 Distinct Player Layouts)
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
                    .testTag("settings_player_themes_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsPlayerThemesHeader,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = strings.settingsPlayerThemesSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 2
                    ) {
                        PlayerThemeStyle.entries.forEach { pStyle ->
                            PlayerThemeMiniPreviewCard(
                                style = pStyle,
                                isSelected = pStyle == selectedPlayerThemeStyle,
                                isArabic = strings.isArabic,
                                onClick = {
                                    ThemePreferences.savePlayerThemeStyle(context, pStyle)
                                    onSelectPlayerThemeStyle(pStyle)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 3. App Design Style & UI Architecture (8 Themes)
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
                    .testTag("settings_design_style_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsDesignStyleHeader,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = strings.settingsDesignStyleSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppDesignStyle.entries.forEach { style ->
                            val isSelected = style == selectedDesignStyle
                            val previewShape = buildDesignShapes(style).medium

                            Surface(
                                shape = previewShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = if (style == AppDesignStyle.GLASS) 0.72f else 0.94f
                                    )
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer.copy(
                                        alpha = if (designTokens.style == AppDesignStyle.GLASS) 0.56f else 0.92f
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(previewShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = if (isSelected) {
                                                listOf(style.badgeColor, style.secondaryBadgeColor)
                                            } else {
                                                listOf(
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                                                )
                                            }
                                        ),
                                        shape = previewShape
                                    )
                                    .clickable { onSelectDesignStyle(style) }
                                    .testTag("design_style_option_${style.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(buildDesignShapes(style).small)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(style.badgeColor, style.secondaryBadgeColor)
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.45f),
                                                shape = buildDesignShapes(style).small
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = designStyleIcon(style),
                                            contentDescription = null,
                                            tint = Color(0xFF0B101C),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (strings.isArabic) style.labelAr else style.labelEn,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (strings.isArabic) style.descAr else style.descEn,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. App Color Theme Palettes (10 Choices)
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsColorPaletteHeader,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 2
                    ) {
                        AppColorPalette.entries.forEach { palette ->
                            val isSelected = palette == selectedPalette
                            val swatch = if (settings.darkTheme) palette.swatchDark else palette.swatchLight
                            val optionShape = MaterialTheme.shapes.small

                            Surface(
                                shape = optionShape,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(optionShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) swatch else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = optionShape
                                    )
                                    .clickable { onSelectPalette(palette) }
                                    .testTag("palette_option_${palette.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(palette.swatchDark, palette.swatchLight)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (strings.isArabic) palette.labelAr else palette.labelEn,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Headphones & Bluetooth Disconnect Behavior
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
                    .testTag("settings_headset_disconnect_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.headsetDisconnectHeader,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = strings.headsetDisconnectSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HeadsetDisconnectOptionChip(
                            title = strings.headsetPauseOptionTitle,
                            subtitle = strings.headsetPauseOptionDesc,
                            icon = Icons.Default.PauseCircle,
                            selected = pauseOnHeadsetDisconnect,
                            onClick = { ThemePreferences.savePauseOnHeadsetDisconnect(context, true) },
                            testTag = "headset_disconnect_pause_option",
                            modifier = Modifier.weight(1f)
                        )
                        HeadsetDisconnectOptionChip(
                            title = strings.headsetContinueOptionTitle,
                            subtitle = strings.headsetContinueOptionDesc,
                            icon = Icons.Default.PlayCircle,
                            selected = !pauseOnHeadsetDisconnect,
                            onClick = { ThemePreferences.savePauseOnHeadsetDisconnect(context, false) },
                            testTag = "headset_disconnect_continue_option",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5b. Audio Equalizer Studio Card
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
                    .testTag("settings_equalizer_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.eqTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = strings.eqOpenStudioDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_equalizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.eqOpenStudioBtn)
                    }
                }
            }
        }

        // 6. Language Switcher
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsLanguageHeader,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    LanguageModeSwitcher(
                        languageCode = settings.languageCode,
                        onLanguageChange = onLanguageChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 7. Automatic Watcher, Quran Isolation & Tab Order
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsStorageHeader,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isScanningStorage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScanningStorage) strings.scanningStorage else strings.autoScanActiveBadge,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (showGrantAudioPermissionButton) {
                        Button(
                            onClick = onRequestAudioPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.enableAutoScanPermissionBtn)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = extColors.quranAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.settingsAutoIsolateDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = settings.autoIsolateQuran,
                            onCheckedChange = { onToggleAutoIsolateQuran() },
                            modifier = Modifier.testTag("auto_isolate_quran_switch")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.excludeUnwantedFoldersTitle,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = strings.excludeUnwantedFoldersDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoExcludeUnwanted,
                            onCheckedChange = { enabled ->
                                FolderFilterStore.setAutoExcludeUnwanted(context, enabled)
                            },
                            modifier = Modifier.testTag("exclude_unwanted_folders_switch")
                        )
                    }

                    OutlinedButton(
                        onClick = { showExcludedFoldersDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_excluded_folders_dialog_button")
                    ) {
                        Icon(Icons.Default.FolderOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.manageExcludedFoldersBtn)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = strings.settingsTabsReorderHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = onResetTabOrder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_tab_order_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.resetTabOrderBtn)
                    }
                }
            }
        }

        // 8. Account & Cloud Sync
        item {
            Card(
                shape = designTokens.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = designTokens.cardContainerColor(
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .naghamDesignBorder()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.settingsAccountHeader,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${settings.displayName} • ${strings.cloudSynced}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.signOutLabel)
                    }
                }
            }
        }
    }

    if (showExcludedFoldersDialog) {
        ManageExcludedFoldersDialog(
            onDismiss = { showExcludedFoldersDialog = false }
        )
    }
}

@Composable
private fun HeadsetDisconnectOptionChip(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun designStyleIcon(style: AppDesignStyle): ImageVector {
    return when (style) {
        AppDesignStyle.GLASS -> Icons.Default.BlurOn
        AppDesignStyle.MATERIAL -> Icons.Default.AutoAwesomeMosaic
        AppDesignStyle.OBSIDIAN_AMOLED -> Icons.Default.Contrast
        AppDesignStyle.VELVET_SUNSET -> Icons.Default.WbTwilight
        AppDesignStyle.CRYSTAL_MINIMAL -> Icons.Default.FilterVintage
        AppDesignStyle.NEON_CYBER -> Icons.Default.Bolt
        AppDesignStyle.ROYAL_ARABESQUE -> Icons.Default.Star
        AppDesignStyle.NEUMORPHIC_STUDIO -> Icons.Default.GraphicEq
    }
}
