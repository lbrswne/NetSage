package com.netsage.app.diagnostic.probe

interface ProbeRunner {
    suspend fun captureNetworkSnapshot(): NetworkSnapshot

    suspend fun run(
        request: ProbeRequest,
        onObservation: suspend (ProbeObservation) -> Unit = {},
    ): ProbeRunResult
}
