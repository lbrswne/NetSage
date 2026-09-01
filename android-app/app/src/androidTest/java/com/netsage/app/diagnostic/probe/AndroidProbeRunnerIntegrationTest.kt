package com.netsage.app.diagnostic.probe

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.net.InetAddress
import java.net.ServerSocket
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidProbeRunnerIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun localHttpServerCompletesDnsTcpAndHttpProbes() = runBlocking {
        withServer {
            enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "text/plain"))

            val result = runner().run(httpRequest(port = port))

            assertEquals(ProbeStatus.SUCCESS, result.observation(ProbeKind.DNS).status)
            assertEquals(ProbeStatus.SUCCESS, result.observation(ProbeKind.TCP).status)
            assertEquals(ProbeStatus.SKIPPED, result.observation(ProbeKind.TLS).status)
            assertEquals(ProbeStatus.SUCCESS, result.observation(ProbeKind.HTTP).status)
            assertEquals("200", result.observation(ProbeKind.HTTP).evidence["statusCode"])
        }
    }

    @Test
    fun sameHostRedirectIsFollowedToFinalResponse() = runBlocking {
        withServer {
            enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/final"))
            enqueue(MockResponse().setResponseCode(204))

            val result = runner().run(httpRequest(port = port, path = "/start"))
            val http = result.observation(ProbeKind.HTTP)

            assertEquals(ProbeStatus.SUCCESS, http.status)
            assertEquals("1", http.evidence["redirectCount"])
            assertEquals("302 -> 204", http.evidence["statusChain"])
            assertTrue(http.evidence["finalUrl"].orEmpty().endsWith("/final"))
            assertEquals("/start", serverTakeRequest(timeoutSeconds = 2)?.path)
            assertEquals("/final", serverTakeRequest(timeoutSeconds = 2)?.path)
        }
    }

    @Test
    fun closedLocalPortIsReportedAsConnectionRefused() = runBlocking {
        val closedPort = ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { it.localPort }

        val result = runner().run(httpRequest(port = closedPort))
        val tcp = result.observation(ProbeKind.TCP)

        assertEquals(ProbeStatus.FAILURE, tcp.status)
        assertEquals("CONNECTION_REFUSED", tcp.error?.code)
    }

    @Test
    fun nonRespondingHttpServerTimesOutWithoutBlockingOtherSteps() = runBlocking {
        withServer {
            enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

            val result = runner().run(
                httpRequest(
                    port = port,
                    timeouts = ProbeTimeouts(
                        dnsMillis = 1_000,
                        tcpMillis = 1_000,
                        tlsMillis = 1_000,
                        httpMillis = 300,
                    ),
                ),
            )

            assertEquals(ProbeStatus.SUCCESS, result.observation(ProbeKind.TCP).status)
            assertEquals(ProbeStatus.TIMEOUT, result.observation(ProbeKind.HTTP).status)
            assertTrue("The bounded timeout should finish promptly", result.durationMillis < 2_000)
        }
    }

    @Test
    fun selfSignedTlsServerIsReportedAsCertificateChainError() = runBlocking {
        val certificate = HeldCertificate.Builder()
            .commonName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()

        withServer(beforeStart = { useHttps(serverCertificates.sslSocketFactory(), false) }) {
            enqueue(MockResponse().setResponseCode(200))

            val result = runner().run(httpsRequest(port))
            val tls = result.observation(ProbeKind.TLS)

            assertEquals(ProbeStatus.FAILURE, tls.status)
            assertEquals("TLS_CHAIN_ERROR", tls.error?.code)
        }
    }

    @Test
    fun cancellingAnInFlightHttpProbePublishesCancelledObservation() = runBlocking {
        withServer {
            enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val observations = Collections.synchronizedList(mutableListOf<ProbeObservation>())

            val job = launch {
                runner().run(
                    httpRequest(
                        port = port,
                        timeouts = ProbeTimeouts(httpMillis = 10_000),
                    ),
                ) { observations += it }
            }

            val request = withContext(Dispatchers.IO) { serverTakeRequest(timeoutSeconds = 5) }
            assertNotNull("The local server should receive the HTTP request before cancellation", request)
            delay(100)
            job.cancel()
            job.join()
            val observationCountAfterCancellation = observations.size
            delay(200)

            assertTrue(job.isCancelled)
            assertTrue(observations.any { it.kind == ProbeKind.HTTP && it.status == ProbeStatus.CANCELLED })
            assertEquals(observationCountAfterCancellation, observations.size)
        }
    }

    private fun runner() = AndroidProbeRunner(
        context = context,
        snapshotReader = NetworkSnapshotReader { connectedSnapshot() },
    )

    private fun httpRequest(
        port: Int,
        path: String = "/",
        timeouts: ProbeTimeouts = ProbeTimeouts(),
    ) = ProbeRequest(
        target = ProbeTarget(
            host = "127.0.0.1",
            port = port,
            scheme = ProbeScheme.HTTP,
            path = path,
        ),
        timeouts = timeouts,
    )

    private fun httpsRequest(port: Int) = ProbeRequest(
        target = ProbeTarget(
            host = "127.0.0.1",
            port = port,
            scheme = ProbeScheme.HTTPS,
        ),
        timeouts = ProbeTimeouts(
            dnsMillis = 1_000,
            tcpMillis = 1_000,
            tlsMillis = 2_000,
            httpMillis = 2_000,
        ),
    )

    private suspend fun <T> withServer(
        beforeStart: MockWebServer.() -> Unit = {},
        block: suspend MockWebServer.() -> T,
    ): T {
        val server = MockWebServer()
        return try {
            server.beforeStart()
            // Android may return ::1 from getLoopbackAddress(). Bind explicitly to IPv4
            // because the probe target is the numeric IPv4 address 127.0.0.1.
            server.start(InetAddress.getByName("127.0.0.1"), 0)
            server.block()
        } finally {
            server.shutdown()
        }
    }

    private fun MockWebServer.serverTakeRequest(timeoutSeconds: Long) =
        takeRequest(timeoutSeconds, TimeUnit.SECONDS)

    private fun ProbeRunResult.observation(kind: ProbeKind): ProbeObservation =
        observations.single { it.kind == kind }

    private fun connectedSnapshot() = NetworkSnapshot(
        capturedAtEpochMillis = System.currentTimeMillis(),
        connected = true,
        internetCapable = true,
        validated = true,
        captivePortal = false,
        metered = false,
        transports = setOf(NetworkTransport.WIFI),
        interfaceName = "test-loopback",
        ipAddresses = listOf("127.0.0.1/8"),
        gateways = listOf("127.0.0.1"),
        dnsServers = listOf("127.0.0.1"),
        mtu = 65_536,
        privateDnsActive = false,
        privateDnsServerName = null,
        proxyHost = null,
        proxyPort = null,
    )
}
