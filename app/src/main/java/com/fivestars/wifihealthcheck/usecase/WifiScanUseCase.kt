package com.fivestars.wifihealthcheck.usecase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import com.fivestars.wifihealthcheck.model.Network
import com.fivestars.wifihealthcheck.util.frequenctyToChannel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

typealias WifiScanData = List<List<Network>>

class WifiScanUseCase(private val wifiManager: WifiManager) {

    private lateinit var currentContinuation: CancellableContinuation<WifiScanData>

    suspend fun wifiScan(context: Context): WifiScanData {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        return suspendCancellableCoroutine { cont ->
            currentContinuation = cont
            wifiManager.startScan()
        }
    }

    fun shutdown(context: Context) {
        context.unregisterReceiver(wifiScanReceiver)
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
        currentContinuation.resumeWith(Result.success(channels))
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            if (success) {
                scanSuccess()
            } else {
                Log.d("wifi", "Scan Failure")
            }
        }
    }

}
