package com.netsage.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netsage.app.ui.NetSageApp
import com.netsage.app.ui.theme.NetSageTheme
import com.netsage.app.util.AppearancePrefs
import com.netsage.app.util.ThemeModeOption

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var appearanceSettings by mutableStateOf(AppearancePrefs.load(this))
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (appearanceSettings.themeMode) {
                ThemeModeOption.SYSTEM -> systemDark
                ThemeModeOption.LIGHT -> false
                ThemeModeOption.DARK -> true
            }
            NetSageTheme(appearanceSettings = appearanceSettings, darkTheme = useDarkTheme) {
                NetSageApp(
                    appearanceSettings = appearanceSettings,
                    onUpdateAppearanceSettings = { updated ->
                        appearanceSettings = updated
                        AppearancePrefs.save(this, updated)
                    }
                )
            }
        }
    }
}
