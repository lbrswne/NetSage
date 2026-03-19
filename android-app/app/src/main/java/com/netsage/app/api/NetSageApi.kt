package com.netsage.app.api

import com.netsage.app.model.DiagnoseRequest
import com.netsage.app.model.DiagnoseResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface NetSageApi {
    @POST("/diagnose")
    suspend fun diagnose(@Body req: DiagnoseRequest): DiagnoseResponse
}
