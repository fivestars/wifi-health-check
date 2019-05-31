package com.fivestars.wifihealthcheck.usecase

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager

class WifiInfoUseCase(private val wifiManager: WifiManager) {
    fun wifiInfo(): WifiInfo{
        return wifiManager.connectionInfo
    }
}