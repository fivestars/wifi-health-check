package com.fivestars.wifihealthcheck.model

import android.net.wifi.WifiInfo
import com.fivestars.wifihealthcheck.usecase.SpeedTestResults
import com.fivestars.wifihealthcheck.usecase.WifiScanData

data class NetworkInfo(
    val interfaceFlags: List<String>,
    val macAddress: String,
    val networkName: String,
    val rxData: Map<String, Int>,
    val txData: Map<String, Int>
)

data class Network(
    val ssid: String,
    val rssi: Int
)

data class AllTheData(
    val networkInfo: NetworkInfo,
    val wifiInfo: WifiInfo,
    val wifiScanData: WifiScanData,
    val speedTestResults: SpeedTestResults?,
    val packetLoss: Double
)
