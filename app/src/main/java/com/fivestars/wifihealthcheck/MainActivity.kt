package com.fivestars.wifihealthcheck

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.MutableInt
import android.view.View
import com.fivestars.wifihealthcheck.model.NetworkInfo
import com.fivestars.wifihealthcheck.usecase.SpeedTestResults
import com.fivestars.wifihealthcheck.usecase.WifiScanData
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
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
        // returning data from a presenter is a pet peeve and also breaks command/query <3
        val data = presenter.execute()

        showResults(data.networkInfo, data.wifiInfo, data.wifiScanData, data.speedTestResults)
    }

    private fun showResults(networkInfo: NetworkInfo, wifiInfo: WifiInfo, wifiScanData: WifiScanData, speedTestResults: SpeedTestResults?) {
        progress_frame_layout.visibility = View.GONE
        results_layout.visibility = View.VISIBLE

        network_info.text = networkInfo.toString()
        wifi_info.text = wifiInfo.toString()
        wifi_scan.text = wifiScanData.toString()
        speed_results.text = speedTestResults.toString()

        showChart(wifiScanData)
    }

    private fun showChart(wifiScanData: WifiScanData) {
        val xAxis = chart1.xAxis
        xAxis.granularity = 1f
        xAxis.labelCount = 11
        xAxis.axisMinimum = .5f

        val values = mutableListOf<BarEntry>()

        for (i in wifiScanData.indices) {
            val signals = arrayListOf<Float>()
            wifiScanData[i].forEach { signals.add(100f + it.rssi) }
            values.add(BarEntry((i + 1).toFloat(), signals.sortedDescending().toFloatArray()))
        }

        val dataSet = BarDataSet(values, "Signal Strength")
        dataSet.colors = getColors()
        chart1.data = BarData(dataSet)
        chart1.setPinchZoom(false)
        chart1.description.isEnabled = false
    }

    private fun getColors(): MutableList<Int> {
        val colors = IntArray(5)
        System.arraycopy(ColorTemplate.JOYFUL_COLORS, 0, colors, 0, 5)
        return colors.toMutableList()
    }
}