package com.netsage.app

import android.content.Intent
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
    private var sharedText: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedText = intent.extractSharedText()
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
                    sharedText = sharedText,
                    onSharedTextConsumed = { sharedText = null },
                    onUpdateAppearanceSettings = { updated ->
                        appearanceSettings = updated
                        AppearancePrefs.save(this, updated)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedText = intent.extractSharedText()
    }

    private fun Intent.extractSharedText(): String? {
        if (action != Intent.ACTION_SEND || type != "text/plain") return null
        return getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    }
}
