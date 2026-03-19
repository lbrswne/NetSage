package com.netsage.app.repo

import com.netsage.app.api.NetSageApi
import com.netsage.app.model.DiagnoseRequest
import com.netsage.app.model.DiagnoseResponse

class DiagnoseRepository(private val api: NetSageApi) {
    suspend fun diagnose(logText: String): DiagnoseResponse {
        return api.diagnose(DiagnoseRequest(log_text = logText))
    }
}
