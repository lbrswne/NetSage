package com.netsage.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netsage.app.ui.NetSageApp
import com.netsage.app.ui.theme.NetSageTheme
import com.netsage.app.util.AppearancePrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var appearanceSettings by mutableStateOf(AppearancePrefs.load(this))
            NetSageTheme(appearanceSettings = appearanceSettings) {
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
