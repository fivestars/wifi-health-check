package com.fivestars.wifihealthcheck

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.fivestars.wifihealthcheck.model.NetworkInfo
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

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
            presenter.wifiScan()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 0x12345) {
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }
            presenter.wifiScan()
        }
    }

    fun showResults(networkInfo: NetworkInfo, wifiInfo: WifiInfo) {
        progress_frame_layout.visibility = View.GONE
        results_layout.visibility = View.VISIBLE

        network_info.text = networkInfo.toString()
        wifi_info.text = wifiInfo.toString()
    }
}