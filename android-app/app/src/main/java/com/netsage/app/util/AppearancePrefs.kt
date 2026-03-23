package com.netsage.app.util

import android.content.Context

enum class FontScaleOption(val label: String, val scale: Float) {
    SMALL("小号", 0.92f),
    NORMAL("标准", 1.0f),
    LARGE("大号", 1.1f),
    EXTRA_LARGE("特大", 1.2f)
}

enum class ThemeStyleOption(val label: String) {
    DEFAULT("默认蓝"),
    FOREST("森林绿"),
    SUNSET("暖日橙")
}

enum class ThemeModeOption(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色")
}

data class AppearanceSettings(
    val fontScale: FontScaleOption = FontScaleOption.NORMAL,
    val themeStyle: ThemeStyleOption = ThemeStyleOption.DEFAULT,
    val themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
)

object AppearancePrefs {
    private const val PREFS_NAME = "netsage_appearance"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_THEME_STYLE = "theme_style"
    private const val KEY_THEME_MODE = "theme_mode"

    fun load(context: Context): AppearanceSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fontScale = prefs.getString(KEY_FONT_SCALE, FontScaleOption.NORMAL.name)
            ?.let { name -> FontScaleOption.entries.firstOrNull { it.name == name } }
            ?: FontScaleOption.NORMAL
        val themeStyle = prefs.getString(KEY_THEME_STYLE, ThemeStyleOption.DEFAULT.name)
            ?.let { name -> ThemeStyleOption.entries.firstOrNull { it.name == name } }
            ?: ThemeStyleOption.DEFAULT
        val themeMode = prefs.getString(KEY_THEME_MODE, ThemeModeOption.SYSTEM.name)
            ?.let { name -> ThemeModeOption.entries.firstOrNull { it.name == name } }
            ?: ThemeModeOption.SYSTEM
        return AppearanceSettings(
            fontScale = fontScale,
            themeStyle = themeStyle,
            themeMode = themeMode,
        )
    }

    fun save(context: Context, settings: AppearanceSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FONT_SCALE, settings.fontScale.name)
            .putString(KEY_THEME_STYLE, settings.themeStyle.name)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .apply()
    }
}
