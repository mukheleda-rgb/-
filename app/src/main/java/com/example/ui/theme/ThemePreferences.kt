package com.example.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreferences {
    private const val PREFS_NAME = "nagham_theme_prefs"
    private const val KEY_DARK_THEME = "dark_theme"
    private const val KEY_HAS_EXPLICIT_THEME = "has_explicit_theme"
    private const val KEY_LANGUAGE = "language_code"
    private const val KEY_COLOR_PALETTE = "color_palette"
    private const val KEY_PLAYER_EFFECT = "player_visual_effect"
    private const val KEY_PLAYER_THEME_STYLE = "player_theme_style"
    private const val KEY_PAUSE_ON_HEADSET_DISCONNECT = "pause_on_headset_disconnect"
    private const val KEY_DESIGN_STYLE = "app_design_style"
    private const val KEY_TAB_ORDER = "main_tab_order_v2"
    private const val KEY_EQ_ENABLED = "eq_master_enabled"
    private const val KEY_EQ_VERTICAL_MODE = "eq_vertical_fader_mode"

    val DEFAULT_TABS = listOf(
        "home",
        "library",
        "playlists",
        "quran_sanctuary",
        "settings"
    )

    private val _playerThemeStyleFlow = MutableStateFlow(PlayerThemeStyle.AURORA_RING)
    val playerThemeStyleFlow: StateFlow<PlayerThemeStyle> = _playerThemeStyleFlow.asStateFlow()

    private val _pauseOnHeadsetDisconnectFlow = MutableStateFlow(true)
    val pauseOnHeadsetDisconnectFlow: StateFlow<Boolean> = _pauseOnHeadsetDisconnectFlow.asStateFlow()

    private val _eqEnabledFlow = MutableStateFlow(true)
    val eqEnabledFlow: StateFlow<Boolean> = _eqEnabledFlow.asStateFlow()

    private val _eqVerticalModeFlow = MutableStateFlow(false)
    val eqVerticalModeFlow: StateFlow<Boolean> = _eqVerticalModeFlow.asStateFlow()

    fun hasExplicitTheme(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_EXPLICIT_THEME, false)
    }

    fun getDarkTheme(context: Context, fallback: Boolean = true): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK_THEME, fallback)
    }

    fun saveDarkTheme(context: Context, isDark: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK_THEME, isDark)
            .putBoolean(KEY_HAS_EXPLICIT_THEME, true)
            .apply()
    }

    fun getLanguageCode(context: Context, fallback: String = "ar"): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, fallback) ?: fallback
    }

    fun saveLanguageCode(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, languageCode)
            .apply()
    }

    fun getColorPalette(
        context: Context,
        fallback: AppColorPalette = AppColorPalette.AMBER_GOLD
    ): AppColorPalette {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_COLOR_PALETTE, fallback.id)
            .orEmpty()
        return AppColorPalette.fromId(id)
    }

    fun saveColorPalette(context: Context, palette: AppColorPalette) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COLOR_PALETTE, palette.id)
            .apply()
    }

    fun getPlayerEffect(context: Context): PlayerVisualEffect {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLAYER_EFFECT, PlayerVisualEffect.VINYL_AURA.id)
            .orEmpty()
        return PlayerVisualEffect.fromId(id)
    }

    fun savePlayerEffect(context: Context, effect: PlayerVisualEffect) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PLAYER_EFFECT, effect.id)
            .apply()
    }

    fun getPlayerThemeStyle(
        context: Context,
        fallback: PlayerThemeStyle = PlayerThemeStyle.AURORA_RING
    ): PlayerThemeStyle {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLAYER_THEME_STYLE, fallback.id)
            .orEmpty()
        val resolved = PlayerThemeStyle.fromId(id)
        _playerThemeStyleFlow.value = resolved
        return resolved
    }

    fun savePlayerThemeStyle(context: Context, style: PlayerThemeStyle) {
        _playerThemeStyleFlow.value = style
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PLAYER_THEME_STYLE, style.id)
            .apply()
    }

    fun getPauseOnHeadsetDisconnect(
        context: Context,
        fallback: Boolean = true
    ): Boolean {
        val resolved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PAUSE_ON_HEADSET_DISCONNECT, fallback)
        _pauseOnHeadsetDisconnectFlow.value = resolved
        return resolved
    }

    fun savePauseOnHeadsetDisconnect(context: Context, pauseOnDisconnect: Boolean) {
        _pauseOnHeadsetDisconnectFlow.value = pauseOnDisconnect
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PAUSE_ON_HEADSET_DISCONNECT, pauseOnDisconnect)
            .apply()
    }

    fun getDesignStyle(
        context: Context,
        fallback: AppDesignStyle = AppDesignStyle.MATERIAL
    ): AppDesignStyle {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DESIGN_STYLE, fallback.id)
            .orEmpty()
        return AppDesignStyle.fromId(id)
    }

    fun saveDesignStyle(context: Context, style: AppDesignStyle) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DESIGN_STYLE, style.id)
            .apply()
    }

    fun getTabOrder(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TAB_ORDER, null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it in DEFAULT_TABS }
            ?.distinct()
            .orEmpty()
        val missing = DEFAULT_TABS.filter { it !in raw }
        return if (raw.isEmpty()) DEFAULT_TABS else raw + missing
    }

    fun saveTabOrder(context: Context, order: List<String>) {
        val valid = order.filter { it in DEFAULT_TABS }.distinct()
        val missing = DEFAULT_TABS.filter { it !in valid }
        val normalized = (valid + missing).joinToString(",")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAB_ORDER, normalized)
            .apply()
    }

    fun getEqEnabled(context: Context, fallback: Boolean = true): Boolean {
        val resolved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EQ_ENABLED, fallback)
        _eqEnabledFlow.value = resolved
        return resolved
    }

    fun saveEqEnabled(context: Context, enabled: Boolean) {
        _eqEnabledFlow.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EQ_ENABLED, enabled)
            .apply()
    }

    fun getEqVerticalMode(context: Context, fallback: Boolean = false): Boolean {
        val resolved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EQ_VERTICAL_MODE, fallback)
        _eqVerticalModeFlow.value = resolved
        return resolved
    }

    fun saveEqVerticalMode(context: Context, verticalMode: Boolean) {
        _eqVerticalModeFlow.value = verticalMode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EQ_VERTICAL_MODE, verticalMode)
            .apply()
    }
}
