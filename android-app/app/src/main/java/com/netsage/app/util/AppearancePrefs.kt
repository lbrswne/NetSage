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

data class AppearanceSettings(
    val fontScale: FontScaleOption = FontScaleOption.NORMAL,
    val themeStyle: ThemeStyleOption = ThemeStyleOption.DEFAULT,
)

object AppearancePrefs {
    private const val PREFS_NAME = "netsage_appearance"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_THEME_STYLE = "theme_style"

    fun load(context: Context): AppearanceSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fontScale = prefs.getString(KEY_FONT_SCALE, FontScaleOption.NORMAL.name)
            ?.let { name -> FontScaleOption.entries.firstOrNull { it.name == name } }
            ?: FontScaleOption.NORMAL
        val themeStyle = prefs.getString(KEY_THEME_STYLE, ThemeStyleOption.DEFAULT.name)
            ?.let { name -> ThemeStyleOption.entries.firstOrNull { it.name == name } }
            ?: ThemeStyleOption.DEFAULT
        return AppearanceSettings(fontScale = fontScale, themeStyle = themeStyle)
    }

    fun save(context: Context, settings: AppearanceSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FONT_SCALE, settings.fontScale.name)
            .putString(KEY_THEME_STYLE, settings.themeStyle.name)
            .apply()
    }
}
