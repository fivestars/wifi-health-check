package com.fivestars.wifihealthcheck

import android.net.wifi.WifiManager
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiInfo
import com.fivestars.wifihealthcheck.model.NetworkInfo
import com.fivestars.wifihealthcheck.usecase.*

data class AllTheData(
    val networkInfo: NetworkInfo,
    val wifiInfo: WifiInfo,
    val wifiScanData: WifiScanData,
    val speedTestResults: SpeedTestResults?
)

class WifiHealthPresenter(private val mainActivity: MainActivity) {

    private val wifiManager = mainActivity.getSystemService(WIFI_SERVICE) as WifiManager

    private val networkInfoUseCase = NetworkInfoUseCase()
    private val wifiInfoUseCase = WifiInfoUseCase(wifiManager)
    private val wifiScanUseCase = WifiScanUseCase(wifiManager)
    private val speedTestUseCase = SpeedTestUseCase()

    suspend fun execute(): AllTheData {
        //val speedResults = speedTestUseCase.speedTest()

        val networkInfo = networkInfoUseCase.getNetworkInfo()
        val wifiInfo = wifiInfoUseCase.wifiInfo()
        val wifiScanData = wifiScanUseCase.wifiScan(mainActivity)

        // validate results
        // RSSI > -60 dBm
        // Speed > 5 Mbps
        // Packet Loss < 5%
        // Link Rate > 43 Mbps
        return AllTheData(networkInfo, wifiInfo, wifiScanData, null)
    }

    fun shutDown() {
        wifiScanUseCase.shutdown(mainActivity)
    }

}
