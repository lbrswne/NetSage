package com.netsage.app.diagnostic.probe

import android.annotation.TargetApi
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.os.CancellationSignal
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.Executor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class DnsResolutionException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/** Keeps API 29 DNS classes out of the code path loaded on Android 8.x and 9. */
@TargetApi(29)
internal object Api29DnsResolver {
    private val directExecutor = Executor { command -> command.run() }

    suspend fun resolve(
        connectivityManager: ConnectivityManager,
        host: String,
    ): List<InetAddress> = suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }

        DnsResolver.getInstance().query(
            connectivityManager.activeNetwork,
            host,
            DnsResolver.FLAG_EMPTY,
            directExecutor,
            cancellationSignal,
            object : DnsResolver.Callback<List<InetAddress>> {
                override fun onAnswer(answer: List<InetAddress>, rcode: Int) {
                    if (!continuation.isActive) return
                    if (rcode == 0 && answer.isNotEmpty()) {
                        continuation.resume(answer)
                    } else {
                        continuation.resumeWithException(
                            DnsResolutionException(
                                code = rcode.toErrorCode(),
                                message = "DNS response for $host returned rcode=$rcode",
                            )
                        )
                    }
                }

                override fun onError(error: DnsResolver.DnsException) {
                    if (!continuation.isActive) return
                    continuation.resumeWithException(
                        DnsResolutionException(
                            code = "DNS_RESOLVER_${error.code}",
                            message = error.message ?: "Android DNS resolver failed for $host",
                            cause = error,
                        )
                    )
                }
            },
        )
    }

    private fun Int.toErrorCode(): String = when (this) {
        2 -> "DNS_SERVFAIL"
        3 -> "DNS_NXDOMAIN"
        5 -> "DNS_REFUSED"
        else -> "DNS_RCODE_$this"
    }
}
