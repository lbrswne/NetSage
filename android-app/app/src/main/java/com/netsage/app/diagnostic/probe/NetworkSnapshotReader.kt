package com.netsage.app.diagnostic.probe

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

fun interface NetworkSnapshotReader {
    suspend fun read(): NetworkSnapshot
}

class AndroidNetworkSnapshotReader(context: Context) : NetworkSnapshotReader {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override suspend fun read(): NetworkSnapshot {
        val capturedAt = System.currentTimeMillis()
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
            val linkProperties = network?.let(connectivityManager::getLinkProperties)
            val proxy = linkProperties?.httpProxy

            NetworkSnapshot(
                capturedAtEpochMillis = capturedAt,
                connected = network != null && capabilities != null,
                internetCapable = capabilities?.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                ) == true,
                validated = capabilities?.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                ) == true,
                captivePortal = capabilities?.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL
                ) == true,
                metered = network != null && connectivityManager.isActiveNetworkMetered,
                transports = capabilities.toTransports(),
                interfaceName = linkProperties?.interfaceName,
                ipAddresses = linkProperties?.linkAddresses
                    ?.mapNotNull { linkAddress ->
                        linkAddress.address.hostAddress?.let { "$it/${linkAddress.prefixLength}" }
                    }
                    ?.distinct()
                    .orEmpty(),
                gateways = linkProperties?.routes
                    ?.filter { it.isDefaultRoute }
                    ?.mapNotNull { it.gateway?.hostAddress }
                    ?.distinct()
                    .orEmpty(),
                dnsServers = linkProperties?.dnsServers
                    ?.mapNotNull { it.hostAddress }
                    ?.distinct()
                    .orEmpty(),
                mtu = linkProperties?.mtu?.takeIf { it > 0 },
                privateDnsActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    linkProperties?.isPrivateDnsActive == true
                } else {
                    false
                },
                privateDnsServerName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    linkProperties?.privateDnsServerName
                } else {
                    null
                },
                proxyHost = proxy?.host,
                proxyPort = proxy?.port?.takeIf { it >= 0 },
            )
        } catch (error: SecurityException) {
            unavailableSnapshot(capturedAt, "Missing network-state permission: ${error.message.orEmpty()}")
        } catch (error: RuntimeException) {
            unavailableSnapshot(capturedAt, "Unable to read active network: ${error.message.orEmpty()}")
        }
    }

    private fun unavailableSnapshot(capturedAt: Long, error: String) = NetworkSnapshot(
        capturedAtEpochMillis = capturedAt,
        connected = false,
        internetCapable = false,
        validated = false,
        captivePortal = false,
        metered = false,
        transports = emptySet(),
        interfaceName = null,
        ipAddresses = emptyList(),
        gateways = emptyList(),
        dnsServers = emptyList(),
        mtu = null,
        privateDnsActive = false,
        privateDnsServerName = null,
        proxyHost = null,
        proxyPort = null,
        error = error,
    )

    private fun NetworkCapabilities?.toTransports(): Set<NetworkTransport> {
        if (this == null) return emptySet()
        val transports = linkedSetOf<NetworkTransport>()
        if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) transports += NetworkTransport.WIFI
        if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) transports += NetworkTransport.CELLULAR
        if (hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) transports += NetworkTransport.ETHERNET
        if (hasTransport(NetworkCapabilities.TRANSPORT_VPN)) transports += NetworkTransport.VPN
        if (hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) transports += NetworkTransport.BLUETOOTH
        if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE)) transports += NetworkTransport.WIFI_AWARE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
            hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN)
        ) {
            transports += NetworkTransport.LOWPAN
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            hasTransport(NetworkCapabilities.TRANSPORT_USB)
        ) {
            transports += NetworkTransport.USB
        }
        if (transports.isEmpty()) transports += NetworkTransport.OTHER
        return transports
    }
}
