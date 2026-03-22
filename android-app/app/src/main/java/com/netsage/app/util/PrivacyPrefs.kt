package com.netsage.app.util

import android.content.Context

private const val PRIVACY_PREFS = "netsage_privacy_prefs"
private const val KEY_PRIVACY_VERSION = "privacy_version"
private const val LEGACY_KEY_PRIVACY_AGREED = "privacy_agreed"
private const val CURRENT_PRIVACY_VERSION = 3

object PrivacyPrefs {
    fun hasAgreed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(LEGACY_KEY_PRIVACY_AGREED) && !prefs.contains(KEY_PRIVACY_VERSION)) {
            prefs.edit().clear().apply()
            return false
        }
        return prefs.getInt(KEY_PRIVACY_VERSION, 0) >= CURRENT_PRIVACY_VERSION
    }

    fun setAgreed(context: Context, agreed: Boolean) {
        context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PRIVACY_VERSION, if (agreed) CURRENT_PRIVACY_VERSION else 0)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
