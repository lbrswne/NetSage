package com.netsage.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.netsage.app.model.DiagnoseHistoryRecord

object DiagnoseHistoryStore {
    private const val PREF_NAME = "netsage_history"
    private const val KEY_HISTORY = "history_list"
    private const val MAX_SIZE = 80

    private val gson = Gson()

    fun load(context: Context): List<DiagnoseHistoryRecord> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<DiagnoseHistoryRecord>>() {}.type
            gson.fromJson<List<DiagnoseHistoryRecord>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, record: DiagnoseHistoryRecord) {
        val merged = (listOf(record) + load(context)).take(MAX_SIZE)
        persist(context, merged)
    }

    fun clear(context: Context) {
        persist(context, emptyList())
    }

    private fun persist(context: Context, list: List<DiagnoseHistoryRecord>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }
}
