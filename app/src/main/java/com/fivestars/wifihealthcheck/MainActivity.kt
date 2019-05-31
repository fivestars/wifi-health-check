package com.fivestars.wifihealthcheck

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.fivestars.wifihealthcheck.model.NetworkInfo
import com.fivestars.wifihealthcheck.usecase.SpeedTestResults
import com.fivestars.wifihealthcheck.usecase.WifiScanData
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var presenter : WifiHealthPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        presenter = WifiHealthPresenter(this)
        requestWifiPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.shutDown()
    }

    private fun requestWifiPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                0x12345
            )
        } else {
            doTheThing()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 0x12345) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            doTheThing()
        }
    }

    private fun doTheThing() = launch {
        val data = presenter.execute()

        showResults(data.networkInfo, data.wifiInfo, data.wifiScanData, data.speedTestResults)
    }

    private fun showResults(networkInfo: NetworkInfo, wifiInfo: WifiInfo, wifiScanData: WifiScanData, speedTestResults: SpeedTestResults) {
        progress_frame_layout.visibility = View.GONE
        results_layout.visibility = View.VISIBLE

        network_info.text = networkInfo.toString()
        wifi_info.text = wifiInfo.toString()
        wifi_scan.text = wifiScanData.toString()
        speed_results.text = speedTestResults.toString()
    }
}