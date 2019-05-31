package com.fivestars.wifihealthcheck.usecase

import com.fivestars.wifihealthcheck.model.NetworkInfo
import com.fivestars.wifihealthcheck.util.executeAsRoot

class NetworkInfoUseCase {

    companion object {
        const val NETWORK_INFO_COMMAND = "ip -s -o link"
    }

    fun getNetworkInfo(): NetworkInfo {
        val networkInfoRaw = executeAsRoot(NETWORK_INFO_COMMAND, lineBreak = "\n")
        val networks = parseNetworkInfo(networkInfoRaw)

        return getMostUsed(networks)
    }

    private fun getMostUsed(networks: List<NetworkInfo>): NetworkInfo {
        return networks.reduce { acc, networkInfo ->
            val maxBytes = acc.rxData["bytes"] ?: 0
            val thisBytes = networkInfo.rxData["bytes"] ?: 0

            if (thisBytes > maxBytes) {
                return@reduce networkInfo
            }

            acc
        }
    }

    private fun parseNetworkInfo(raw: String): List<NetworkInfo> {
        val blocks = raw.split("\n").filter { it.isNotEmpty() }
        val networks = mutableListOf<NetworkInfo>()

        blocks.forEach {
            val lines = it.split("\\").filter { it.isNotEmpty() }

            // interface
            val interfaceStr = lines[0]
            val interfaceInfo: List<String> = if (interfaceStr.isNotEmpty()) {
                interfaceStr.split(" ").filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            // network name
            val networkName = interfaceInfo[1].dropLast(1)

            // interface flags
            val interfaceFlags = if (interfaceInfo[2].isNotEmpty()) {
                interfaceInfo[2].drop(1).dropLast(1).split(",")
            } else {
                emptyList()
            }

            val linkAddresses = if (lines[1].isNotEmpty()) {
                lines[1].split(" ").filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val macAddress = linkAddresses[1]

            val rxHeaders = if (lines[2].isNotEmpty()) {
                lines[2].split(" ").filter { it.isNotEmpty() }.drop(1)
            } else {
                emptyList()
            }

            val rxValues = if (lines[3].isNotEmpty()) {
                lines[3].split(" ").filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val txheaders = if(lines[4].isNotEmpty()) {
                lines[4].split(" ").filter { it.isNotEmpty() }.drop(1)
            } else {
                emptyList()
            }

            val txValues = if (lines[5].isNotEmpty()) {
                lines[5].split(" ").filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val rxData = mutableMapOf<String, Int>()
            rxHeaders.forEachIndexed { index, header ->
                // networkInfo[`rx${rxHeader.charAt(0).toUpperCase()}${rxHeader.slice(1)}`] = Number(rxValues[i]);
                rxData[header] = rxValues[index].toInt()
            }

            val txData = mutableMapOf<String, Int>()
            txheaders.forEachIndexed { index, header ->
                txData[header] = txValues[index].toInt()
            }

            val networkInfo = NetworkInfo(
                interfaceFlags = interfaceFlags,
                macAddress = macAddress,
                networkName = networkName,
                rxData = rxData,
                txData = txData
            )
            networks.add(networkInfo)
        }
        return networks
    }
}