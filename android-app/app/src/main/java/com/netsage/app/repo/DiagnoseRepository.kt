package com.netsage.app.repo

import com.netsage.app.api.NetSageApi
import com.netsage.app.model.DiagnoseRequest
import com.netsage.app.model.DiagnoseResponse
import kotlinx.coroutines.delay

class DiagnoseRepository(private val api: NetSageApi) {
    suspend fun diagnose(logText: String): DiagnoseResponse {
        val req = DiagnoseRequest(log_text = logText)
        var last: Throwable? = null
        val retries = listOf(0L, 400L, 900L) // first try + 2 retries

        for (waitMs in retries) {
            if (waitMs > 0) delay(waitMs)
            runCatching { api.diagnose(req) }
                .onSuccess { return it }
                .onFailure { last = it }
        }

        throw last ?: RuntimeException("unknown request failure")
    }
}
