package com.netsage.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.netsage.app.ui.NetSageApp
import com.netsage.app.ui.theme.NetSageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NetSageTheme {
                NetSageApp()
            }
        }
    }
}
