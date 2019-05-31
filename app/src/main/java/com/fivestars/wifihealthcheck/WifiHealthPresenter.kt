package com.fivestars.wifihealthcheck

import android.content.BroadcastReceiver
import android.content.Context
import android.net.wifi.WifiManager
import com.fivestars.wifihealthcheck.util.executeAsRoot
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiInfo
import android.os.AsyncTask
import android.util.Log
import com.fivestars.wifihealthcheck.model.Network
import com.fivestars.wifihealthcheck.model.NetworkInfo
import com.fivestars.wifihealthcheck.util.frequenctyToChannel
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.IRepeatListener
import java.math.BigDecimal
import java.math.RoundingMode


class WifiHealthPresenter(private val mainActivity: MainActivity) {

    companion object {
        const val NETWORK_INFO_COMMAND = "ip -s -o link"
    }

    private var wifiManager: WifiManager = mainActivity.getSystemService(WIFI_SERVICE) as WifiManager
    private val speedTest = SpeedTestTask()

    init {
        speedTest.execute()
        getNetworkInfo()
        wifiInfo()

        // validate results
        // RSSI > -60 dBm
        // Speed > 5 Mbps
        // Packet Loss < 5%
        // Link Rate > 43 Mbps

        mainActivity.showResults()
    }

    fun shutDown() {
        mainActivity.unregisterReceiver(wifiScanReceiver)
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
//                scanSuccess()
            } else {
                scanFailure()
            }
        }
    }

    private fun scanSuccess() {
        val data = wifiManager.scanResults

        val channels: List<MutableList<Network>> = List(size = 11) {
            mutableListOf<Network>()
        }

        data.forEach { result ->
            val channel = result.frequency.frequenctyToChannel() - 1

            if (result.SSID.isEmpty()) {
                return@forEach
            }

            channels[channel].add(
                Network(
                    ssid = result.SSID,
                    rssi = result.level
                )
            )
        }
        Log.d("hey", "")
    }

    private fun scanFailure() {
        Log.d("scan fail", "")
    }


    fun wifiScan() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        mainActivity?.registerReceiver(wifiScanReceiver, intentFilter)

        val success = wifiManager.startScan()
        if (!success) {
            // scan failure handling
            scanFailure()
        }
    }

    fun getNetworkInfo(): NetworkInfo? {
        val networkInfoRaw = executeAsRoot(NETWORK_INFO_COMMAND, lineBreak = "\n")
        val networks = parseNetworkInfo(networkInfoRaw)

        if (networks.isEmpty()) {
            return null;
        }

        return getMostUsed(networks)
    }

    fun wifiInfo(): WifiInfo {
        return wifiManager.connectionInfo
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

    inner class SpeedTestTask : AsyncTask<Void, Void, String>() {

        private val downloads = mutableListOf<BigDecimal>()

        override fun doInBackground(vararg params: Void): String? {

            val speedTestSocket = SpeedTestSocket()

            val url = "https://ashburn02.speedtest.windstream.net:8080/download?nocache=bce25e94-4178-43f4-939d-dd9587321aad&size=250"

            speedTestSocket.startDownloadRepeat(url, 20000, 2000, object : IRepeatListener {
                override fun onCompletion(report: SpeedTestReport) {

                    val numberOfTests = downloads.size
                    val totalRate = downloads.reduce { acc, bigDecimal ->  acc + bigDecimal}
                    val avg = totalRate.divide(BigDecimal.valueOf(numberOfTests.toDouble()), RoundingMode.HALF_UP)
                    Log.v("speedtest", "[completed] : $avg")
                }

                override fun onReport(report: SpeedTestReport) {
                    var rate = report.transferRateBit
                    val divisor = BigDecimal.valueOf(1000000)
                    rate = rate.divide(divisor, RoundingMode.HALF_UP)
                    downloads.add(rate)
                }
            })

            return null
        }
    }
}
