package com.netsage.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.netsage.app.model.SavedReportItem

object SavedReportStore {
    private const val PREF_NAME = "netsage_saved_reports"
    private const val KEY_ITEMS = "saved_report_items"
    private const val MAX_SIZE = 50

    private val gson = Gson()

    fun load(context: Context): List<SavedReportItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<SavedReportItem>>() {}.type
            gson.fromJson<List<SavedReportItem>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, item: SavedReportItem) {
        val merged = (listOf(item) + load(context)).distinctBy { it.id }.take(MAX_SIZE)
        persist(context, merged)
    }

    private fun persist(context: Context, list: List<SavedReportItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ITEMS, gson.toJson(list)).apply()
    }
}
