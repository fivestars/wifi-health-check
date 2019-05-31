package com.fivestars.wifihealthcheck.model

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