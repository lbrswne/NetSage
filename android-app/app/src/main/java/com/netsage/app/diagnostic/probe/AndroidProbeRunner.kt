package com.netsage.app.diagnostic.probe

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.SystemClock
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.IDN
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AndroidProbeRunner(
    context: Context,
    private val snapshotReader: NetworkSnapshotReader = AndroidNetworkSnapshotReader(context),
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val elapsedClock: () -> Long = SystemClock::elapsedRealtime,
) : ProbeRunner {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override suspend fun captureNetworkSnapshot(): NetworkSnapshot = snapshotReader.read()

    override suspend fun run(
        request: ProbeRequest,
        onObservation: suspend (ProbeObservation) -> Unit,
    ): ProbeRunResult {
        val runStartedAt = wallClock()
        val runStartedElapsed = elapsedClock()
        val snapshot = captureNetworkSnapshot()
        val observations = mutableListOf<ProbeObservation>()
        val validationError = validate(request)

        if (validationError != null) {
            ProbeKind.entries.forEach { kind ->
                val observation = failedObservation(
                    kind = kind,
                    target = request.displayTarget(kind),
                    code = "INVALID_TARGET",
                    message = validationError,
                )
                observations += observation
                onObservation(observation)
            }
            return ProbeRunResult(
                snapshot = snapshot,
                observations = observations,
                startedAtEpochMillis = runStartedAt,
                durationMillis = elapsedSince(runStartedElapsed),
            )
        }

        var preferredAddress: String? = null
        val steps = listOf(
            ProbeStep(ProbeKind.DNS, request.timeouts.dnsMillis) {
                dnsPayload(request.target.normalizedHost())
            },
            ProbeStep(ProbeKind.TCP, request.timeouts.tcpMillis) {
                tcpPayload(request.target, preferredAddress, request.timeouts.tcpMillis)
            },
            ProbeStep(ProbeKind.TLS, request.timeouts.tlsMillis) {
                if (request.target.scheme == ProbeScheme.HTTP) {
                    StepPayload(
                        status = ProbeStatus.SKIPPED,
                        evidence = mapOf("reason" to "TLS is not used for an HTTP target"),
                    )
                } else {
                    tlsPayload(
                        target = request.target,
                        preferredAddress = preferredAddress,
                        connectTimeoutMillis = request.timeouts.tcpMillis,
                        tlsTimeoutMillis = request.timeouts.tlsMillis,
                    )
                }
            },
            ProbeStep(ProbeKind.HTTP, request.timeouts.httpMillis) {
                httpPayload(request.target, request.timeouts.httpMillis)
            },
        )

        for (step in steps) {
            val observation = try {
                executeStep(
                    kind = step.kind,
                    target = request.displayTarget(step.kind),
                    timeoutMillis = step.timeoutMillis,
                    block = step.block,
                )
            } catch (cancelled: ProbeStepCancellation) {
                withContext(NonCancellable) {
                    onObservation(cancelled.observation)
                }
                throw cancelled
            }

            observations += observation
            if (step.kind == ProbeKind.DNS && observation.status == ProbeStatus.SUCCESS) {
                preferredAddress = observation.evidence["addresses"]
                    ?.split(',')
                    ?.firstOrNull()
                    ?.trim()
            }
            onObservation(observation)
        }

        return ProbeRunResult(
            snapshot = snapshot,
            observations = observations,
            startedAtEpochMillis = runStartedAt,
            durationMillis = elapsedSince(runStartedElapsed),
        )
    }

    private suspend fun executeStep(
        kind: ProbeKind,
        target: String,
        timeoutMillis: Long,
        block: suspend () -> StepPayload,
    ): ProbeObservation {
        val startedAt = wallClock()
        val startedElapsed = elapsedClock()
        val boundedTimeout = timeoutMillis.coerceAtLeast(MIN_TIMEOUT_MILLIS)

        return try {
            val payload = withTimeout(boundedTimeout) { block() }
            ProbeObservation(
                kind = kind,
                status = payload.status,
                target = target,
                startedAtEpochMillis = startedAt,
                durationMillis = elapsedSince(startedElapsed),
                evidence = payload.evidence,
                rawEvidence = payload.rawEvidence,
                error = payload.error,
            )
        } catch (timeout: TimeoutCancellationException) {
            ProbeObservation(
                kind = kind,
                status = ProbeStatus.TIMEOUT,
                target = target,
                startedAtEpochMillis = startedAt,
                durationMillis = elapsedSince(startedElapsed),
                error = ProbeError(
                    code = "TIMEOUT",
                    message = "$kind exceeded the ${boundedTimeout} ms timeout",
                    causeType = timeout::class.java.simpleName,
                ),
            )
        } catch (cancelled: CancellationException) {
            val observation = ProbeObservation(
                kind = kind,
                status = ProbeStatus.CANCELLED,
                target = target,
                startedAtEpochMillis = startedAt,
                durationMillis = elapsedSince(startedElapsed),
                error = ProbeError(
                    code = "CANCELLED",
                    message = "$kind was cancelled",
                    causeType = cancelled::class.java.simpleName,
                ),
            )
            throw ProbeStepCancellation(observation, cancelled)
        } catch (error: Exception) {
            val mapped = error.toProbeError(kind)
            ProbeObservation(
                kind = kind,
                status = if (error is SocketTimeoutException) {
                    ProbeStatus.TIMEOUT
                } else {
                    ProbeStatus.FAILURE
                },
                target = target,
                startedAtEpochMillis = startedAt,
                durationMillis = elapsedSince(startedElapsed),
                error = mapped,
            )
        }
    }

    private suspend fun dnsPayload(host: String): StepPayload {
        val addresses = resolve(host)
            .mapNotNull(InetAddress::getHostAddress)
            .distinct()
        if (addresses.isEmpty()) throw UnknownHostException("No address returned for $host")

        val ipv4Count = addresses.count { !it.contains(':') }
        val ipv6Count = addresses.size - ipv4Count
        return StepPayload(
            status = ProbeStatus.SUCCESS,
            evidence = linkedMapOf(
                "addresses" to addresses.joinToString(", "),
                "addressCount" to addresses.size.toString(),
                "ipv4Count" to ipv4Count.toString(),
                "ipv6Count" to ipv6Count.toString(),
            ),
            rawEvidence = addresses.joinToString(separator = "\n"),
        )
    }

    private suspend fun resolve(host: String): List<InetAddress> {
        if (isNumericAddress(host)) {
            return runInterruptible(Dispatchers.IO) { listOf(InetAddress.getByName(host)) }
        }
        val asciiHost = IDN.toASCII(host)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29DnsResolver.resolve(connectivityManager, asciiHost)
        } else {
            runInterruptible(Dispatchers.IO) { InetAddress.getAllByName(asciiHost).toList() }
        }
    }

    private suspend fun tcpPayload(
        target: ProbeTarget,
        preferredAddress: String?,
        timeoutMillis: Long,
    ): StepPayload = runInterruptible(Dispatchers.IO) {
        val connectHost = preferredAddress ?: target.normalizedHost()
        Socket().use { socket ->
            socket.connect(
                InetSocketAddress(connectHost, target.effectivePort),
                timeoutMillis.toIntTimeout(),
            )
            StepPayload(
                status = ProbeStatus.SUCCESS,
                evidence = linkedMapOf(
                    "remoteAddress" to socket.inetAddress.hostAddress.orEmpty(),
                    "remotePort" to socket.port.toString(),
                    "localAddress" to socket.localAddress.hostAddress.orEmpty(),
                    "localPort" to socket.localPort.toString(),
                ),
                rawEvidence = "Connected ${socket.localAddress.hostAddress}:${socket.localPort} -> " +
                    "${socket.inetAddress.hostAddress}:${socket.port}",
            )
        }
    }

    private suspend fun tlsPayload(
        target: ProbeTarget,
        preferredAddress: String?,
        connectTimeoutMillis: Long,
        tlsTimeoutMillis: Long,
    ): StepPayload = runInterruptible(Dispatchers.IO) {
        val host = target.normalizedHost()
        val connectHost = preferredAddress ?: host
        Socket().use { plainSocket ->
            plainSocket.connect(
                InetSocketAddress(connectHost, target.effectivePort),
                connectTimeoutMillis.toIntTimeout(),
            )
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
            (factory.createSocket(plainSocket, host, target.effectivePort, true) as SSLSocket).use { sslSocket ->
                sslSocket.soTimeout = tlsTimeoutMillis.toIntTimeout()
                sslSocket.startHandshake()
                val session = sslSocket.session
                val hostnameVerified = HttpsURLConnection.getDefaultHostnameVerifier()
                    .verify(host, session)
                val certificate = session.peerCertificates.firstOrNull() as? X509Certificate
                    ?: return@runInterruptible StepPayload(
                        status = ProbeStatus.FAILURE,
                        evidence = mapOf("hostnameVerified" to hostnameVerified.toString()),
                        error = ProbeError(
                            code = "PEER_CERTIFICATE_MISSING",
                            message = "TLS peer did not provide an X.509 certificate",
                        ),
                    )

                val validity = certificate.validityAt(Date())
                val success = hostnameVerified && validity == CertificateValidity.VALID
                val evidence = linkedMapOf(
                    "protocol" to session.protocol.orEmpty(),
                    "cipherSuite" to session.cipherSuite.orEmpty(),
                    "hostnameVerified" to hostnameVerified.toString(),
                    "certificateValidity" to validity.name,
                    "subject" to certificate.subjectX500Principal.name,
                    "issuer" to certificate.issuerX500Principal.name,
                    "serialNumber" to certificate.serialNumber.toString(16),
                    "notBefore" to certificate.notBefore.toInstant().toString(),
                    "notAfter" to certificate.notAfter.toInstant().toString(),
                )
                StepPayload(
                    status = if (success) ProbeStatus.SUCCESS else ProbeStatus.FAILURE,
                    evidence = evidence,
                    rawEvidence = evidence.entries.joinToString("\n") { "${it.key}=${it.value}" },
                    error = when {
                        !hostnameVerified -> ProbeError(
                            code = "TLS_HOSTNAME_MISMATCH",
                            message = "Certificate does not match $host",
                        )
                        validity == CertificateValidity.EXPIRED -> ProbeError(
                            code = "TLS_CERTIFICATE_EXPIRED",
                            message = "Certificate expired at ${certificate.notAfter.toInstant()}",
                        )
                        validity == CertificateValidity.NOT_YET_VALID -> ProbeError(
                            code = "TLS_CERTIFICATE_NOT_YET_VALID",
                            message = "Certificate is valid from ${certificate.notBefore.toInstant()}",
                        )
                        else -> null
                    },
                )
            }
        }
    }

    private suspend fun httpPayload(
        target: ProbeTarget,
        timeoutMillis: Long,
    ): StepPayload = runInterruptible(Dispatchers.IO) {
        val deadline = elapsedClock() + timeoutMillis.coerceAtLeast(MIN_TIMEOUT_MILLIS)
        var currentUrl = target.toUrl()
        val redirectChain = mutableListOf<String>()
        val statusChain = mutableListOf<Int>()
        val visited = linkedSetOf<String>()
        var responseMessage = ""
        var contentType = ""
        var firstResponseMillis: Long? = null

        while (true) {
            if (!visited.add(currentUrl.toExternalForm())) {
                return@runInterruptible StepPayload(
                    status = ProbeStatus.FAILURE,
                    evidence = mapOf("redirectChain" to redirectChain.joinToString(" | ")),
                    error = ProbeError("HTTP_REDIRECT_LOOP", "Redirect loop detected at $currentUrl"),
                )
            }

            val remainingMillis = (deadline - elapsedClock()).coerceAtLeast(1)
            val connection = currentUrl.openConnection() as? HttpURLConnection
                ?: throw IOException("Target is not an HTTP connection")
            val responseStarted = elapsedClock()
            try {
                connection.instanceFollowRedirects = false
                connection.requestMethod = "HEAD"
                connection.connectTimeout = remainingMillis.toIntTimeout()
                connection.readTimeout = remainingMillis.toIntTimeout()
                connection.setRequestProperty("User-Agent", "NetSage/0.2 local probe")
                val statusCode = connection.responseCode
                if (firstResponseMillis == null) {
                    firstResponseMillis = elapsedClock() - responseStarted
                }
                statusChain += statusCode
                responseMessage = connection.responseMessage.orEmpty()
                contentType = connection.contentType.orEmpty()
                val location = connection.getHeaderField("Location")
                val shouldRedirect = target.followRedirects &&
                    statusCode in REDIRECT_STATUS_CODES &&
                    !location.isNullOrBlank()

                if (!shouldRedirect) {
                    val evidence = linkedMapOf(
                        "statusCode" to statusCode.toString(),
                        "resultStatus" to httpResultStatus(statusCode),
                        "responseMessage" to responseMessage,
                        "redirected" to redirectChain.isNotEmpty().toString(),
                        "redirectCount" to redirectChain.size.toString(),
                        "redirectChain" to redirectChain.joinToString(" | "),
                        "statusChain" to statusChain.joinToString(" -> "),
                        "finalUrl" to currentUrl.toExternalForm(),
                        "firstResponseMillis" to firstResponseMillis.toString(),
                        "contentType" to contentType,
                        "redirectLimitReached" to (
                            shouldRedirect && redirectChain.size >= target.maxRedirects
                        ).toString(),
                    )
                    return@runInterruptible StepPayload(
                        status = if (statusCode >= 500) ProbeStatus.FAILURE else ProbeStatus.SUCCESS,
                        evidence = evidence,
                        rawEvidence = buildString {
                            appendLine("HTTP $statusCode $responseMessage")
                            redirectChain.forEach(::appendLine)
                            append("finalUrl=$currentUrl")
                        },
                    )
                }

                val nextUrl = URL(currentUrl, location)
                if (nextUrl.protocol != "http" && nextUrl.protocol != "https") {
                    return@runInterruptible StepPayload(
                        status = ProbeStatus.FAILURE,
                        evidence = mapOf(
                            "statusCode" to statusCode.toString(),
                            "location" to location,
                        ),
                        error = ProbeError(
                            "HTTP_UNSUPPORTED_REDIRECT",
                            "Redirect uses unsupported scheme: ${nextUrl.protocol}",
                        ),
                    )
                }
                if (!isSameRedirectHost(target.normalizedHost(), nextUrl.host)) {
                    return@runInterruptible StepPayload(
                        status = ProbeStatus.FAILURE,
                        evidence = mapOf(
                            "statusCode" to statusCode.toString(),
                            "location" to location,
                            "redirectChain" to redirectChain.joinToString(" | "),
                            "blockedRedirectUrl" to nextUrl.toExternalForm(),
                        ),
                        error = ProbeError(
                            "HTTP_CROSS_HOST_REDIRECT_BLOCKED",
                            "Redirect to a different host was not followed: ${nextUrl.host}",
                        ),
                    )
                }
                if (redirectChain.size >= target.maxRedirects) {
                    return@runInterruptible StepPayload(
                        status = ProbeStatus.FAILURE,
                        evidence = mapOf(
                            "statusCode" to statusCode.toString(),
                            "location" to location,
                            "redirectChain" to redirectChain.joinToString(" | "),
                            "redirectLimit" to target.maxRedirects.toString(),
                        ),
                        error = ProbeError(
                            "HTTP_REDIRECT_LIMIT_REACHED",
                            "Same-host redirect limit (${target.maxRedirects}) reached",
                        ),
                    )
                }
                redirectChain += "$statusCode ${currentUrl.toExternalForm()} -> ${nextUrl.toExternalForm()}"
                currentUrl = nextUrl
            } finally {
                connection.disconnect()
            }
        }
        @Suppress("UNREACHABLE_CODE")
        throw IllegalStateException("HTTP probe loop ended unexpectedly")
    }

    private fun validate(request: ProbeRequest): String? {
        val host = request.target.normalizedHost()
        return when {
            host.isEmpty() -> "Host is required"
            host.contains("://") || host.any(Char::isWhitespace) || host.contains('/') ->
                "Host must not include a scheme, path, or whitespace"
            request.target.effectivePort !in 1..65_535 -> "Port must be between 1 and 65535"
            request.target.maxRedirects !in 0..10 -> "maxRedirects must be between 0 and 10"
            request.target.path.contains('#') -> "HTTP path must not contain a URL fragment"
            listOf(
                request.timeouts.dnsMillis,
                request.timeouts.tcpMillis,
                request.timeouts.tlsMillis,
                request.timeouts.httpMillis,
            ).any { it <= 0 } -> "Probe timeouts must be positive"
            else -> null
        }
    }

    private fun ProbeRequest.displayTarget(kind: ProbeKind): String = when (kind) {
        ProbeKind.DNS -> target.normalizedHost()
        ProbeKind.TCP, ProbeKind.TLS -> "${target.normalizedHost()}:${target.effectivePort}"
        ProbeKind.HTTP -> runCatching { target.toUrl().toExternalForm() }.getOrElse {
            "${target.scheme.value}://${target.normalizedHost()}:${target.effectivePort}${target.normalizedPath()}"
        }
    }

    private fun ProbeTarget.toUrl(): URL {
        val normalizedHost = normalizedHost()
        val pathWithQuery = normalizedPath()
        val queryIndex = pathWithQuery.indexOf('?')
        val uriPath = if (queryIndex >= 0) pathWithQuery.substring(0, queryIndex) else pathWithQuery
        val query = if (queryIndex >= 0) pathWithQuery.substring(queryIndex + 1) else null
        return URI(scheme.value, null, normalizedHost, effectivePort, uriPath, query, null).toURL()
    }

    private fun failedObservation(
        kind: ProbeKind,
        target: String,
        code: String,
        message: String,
    ) = ProbeObservation(
        kind = kind,
        status = ProbeStatus.FAILURE,
        target = target,
        startedAtEpochMillis = wallClock(),
        durationMillis = 0,
        error = ProbeError(code, message),
    )

    private fun Throwable.toProbeError(kind: ProbeKind): ProbeError {
        val causes = generateSequence(this) { it.cause }.toList()
        val code = when {
            causes.any { it is CertificateExpiredException } -> "TLS_CERTIFICATE_EXPIRED"
            causes.any { it is CertificateNotYetValidException } -> "TLS_CERTIFICATE_NOT_YET_VALID"
            this is DnsResolutionException -> code
            this is UnknownHostException -> "DNS_LOOKUP_FAILED"
            this is SocketTimeoutException -> "SOCKET_TIMEOUT"
            this is ConnectException -> "CONNECTION_REFUSED"
            this is SSLPeerUnverifiedException -> "TLS_PEER_UNVERIFIED"
            this is SSLHandshakeException -> "TLS_HANDSHAKE_FAILED"
            this is SecurityException -> "PERMISSION_DENIED"
            this is IllegalArgumentException -> "INVALID_TARGET"
            this is IOException -> "IO_ERROR"
            else -> "${kind.name}_FAILED"
        }
        return ProbeError(
            code = code,
            message = message?.takeIf { it.isNotBlank() } ?: "$kind failed",
            causeType = this::class.java.name,
        )
    }

    private fun X509Certificate.validityAt(now: Date): CertificateValidity = try {
        checkValidity(now)
        CertificateValidity.VALID
    } catch (_: CertificateExpiredException) {
        CertificateValidity.EXPIRED
    } catch (_: CertificateNotYetValidException) {
        CertificateValidity.NOT_YET_VALID
    }

    private fun isNumericAddress(host: String): Boolean {
        if (host.contains(':')) return true
        val parts = host.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.all(Char::isDigit) && part.toIntOrNull() in 0..255
        }
    }

    private fun Long.toIntTimeout(): Int = coerceIn(1, Int.MAX_VALUE.toLong()).toInt()

    private fun elapsedSince(startedAt: Long): Long = (elapsedClock() - startedAt).coerceAtLeast(0)

    private data class ProbeStep(
        val kind: ProbeKind,
        val timeoutMillis: Long,
        val block: suspend () -> StepPayload,
    )

    private data class StepPayload(
        val status: ProbeStatus,
        val evidence: Map<String, String> = emptyMap(),
        val rawEvidence: String? = null,
        val error: ProbeError? = null,
    )

    private enum class CertificateValidity {
        VALID,
        EXPIRED,
        NOT_YET_VALID,
    }

    private class ProbeStepCancellation(
        val observation: ProbeObservation,
        cause: CancellationException,
    ) : CancellationException(cause.message) {
        init {
            initCause(cause)
        }
    }

    private companion object {
        const val MIN_TIMEOUT_MILLIS = 100L
        val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
    }
}

/** Kept separate from runtime [ProbeStatus], which has no warning variant. */
internal fun httpResultStatus(statusCode: Int): String = when (statusCode) {
    in 400..499 -> "WARNING"
    in 500..599 -> "FAILED"
    else -> "SUCCESS"
}

/** Redirects are deliberately limited to the host the user entered. */
internal fun isSameRedirectHost(requestedHost: String, redirectHost: String): Boolean =
    canonicalRedirectHost(requestedHost) == canonicalRedirectHost(redirectHost)

private fun canonicalRedirectHost(host: String): String = IDN.toASCII(host.trim().removeSuffix("."))
    .lowercase()
