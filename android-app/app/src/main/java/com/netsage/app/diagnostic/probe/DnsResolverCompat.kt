package com.netsage.app.diagnostic.probe

import android.annotation.TargetApi
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.os.CancellationSignal
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.Executor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class DnsResolutionException(
    val code: String,
    message: String,
    cause: Throwable? = null,
    val evidence: Map<String, String> = emptyMap(),
) : IOException(message, cause)

/** Whether the resolver can report the DNS response RCODE for this lookup. */
internal enum class DnsRcodeAvailability(val evidenceValue: String) {
    AVAILABLE("available"),
    UNAVAILABLE("unavailable"),
    NOT_APPLICABLE("not_applicable"),
}

internal data class DnsResolutionResult(
    val addresses: List<InetAddress>,
    val rcodeAvailability: DnsRcodeAvailability,
    val resolver: String,
)

/**
 * Android 8/9 use [InetAddress], whose API does not expose DNS RCODE values.
 * Keep that limitation visible instead of inferring NXDOMAIN or SERVFAIL.
 */
internal fun inetAddressFallbackFailure(host: String, cause: UnknownHostException): DnsResolutionException =
    DnsResolutionException(
        code = "DNS_LOOKUP_FAILED",
        message = "InetAddress fallback could not resolve $host; DNS RCODE unavailable on this Android version",
        cause = cause,
        evidence = mapOf(
            "dnsRcodeAvailable" to DnsRcodeAvailability.UNAVAILABLE.evidenceValue,
            "resolver" to "InetAddressFallback",
        ),
    )

/** Keeps API 29 DNS classes out of the code path loaded on Android 8.x and 9. */
@TargetApi(29)
internal object Api29DnsResolver {
    private val directExecutor = Executor { command -> command.run() }

    suspend fun resolve(
        connectivityManager: ConnectivityManager,
        host: String,
    ): DnsResolutionResult = suspendCancellableCoroutine { continuation ->
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
                        continuation.resume(
                            DnsResolutionResult(
                                addresses = answer,
                                rcodeAvailability = DnsRcodeAvailability.AVAILABLE,
                                resolver = "DnsResolver",
                            ),
                        )
                    } else {
                        continuation.resumeWithException(
                            DnsResolutionException(
                                code = rcode.toErrorCode(),
                                message = "DNS response for $host returned rcode=$rcode",
                                evidence = mapOf(
                                    "dnsRcodeAvailable" to DnsRcodeAvailability.AVAILABLE.evidenceValue,
                                    "dnsRcode" to rcode.toString(),
                                    "resolver" to "DnsResolver",
                                ),
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
                            evidence = mapOf(
                                "dnsRcodeAvailable" to DnsRcodeAvailability.AVAILABLE.evidenceValue,
                                "resolver" to "DnsResolver",
                            ),
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
