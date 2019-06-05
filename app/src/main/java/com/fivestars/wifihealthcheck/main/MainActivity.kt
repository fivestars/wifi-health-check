package com.fivestars.wifihealthcheck.main

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import com.fivestars.wifihealthcheck.R
import com.fivestars.wifihealthcheck.model.AllTheData
import com.fivestars.wifihealthcheck.usecase.WifiScanData
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
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
        advanced_button.setOnClickListener {
            advanced_results_layout.visibility = View.VISIBLE
            pass_fail_layout.visibility = View.GONE
        }
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
        presenter.execute()
    }

    fun updateProgress(progress: Int) {
        progress_bar.progress = progress
    }

    fun showResults(
        allTheData: AllTheData,
        wifiScanData: WifiScanData,
        pass: Boolean
    ) {
        progress_frame_layout.visibility = View.GONE
        pass_fail_layout.visibility = View.VISIBLE
        advanced_button.visibility = View.VISIBLE

        this.network_info.text = "Network Name: " + allTheData.networkInfo.networkName
        this.wifi_info.text = "Link Speed: " + allTheData.wifiInfo.linkSpeed
        this.speed_results.text = "Download: " +allTheData.speedTestResults?.download + " Upload: " +allTheData.speedTestResults?.upload
        this.packet_loss.text = "Packet loss: " + allTheData.packetLoss

        pass_fail_view.text = when {
            pass -> "Pass"
            else -> "Fail"
        }

        showChart(wifiScanData)
    }


    fun getRandomColor(): Int {
        val rnd = Random()
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
    }

    // each group should be an ssid
    // a list of barentries for each ssid

    private fun showChart(wifiScanData: WifiScanData) {
        val ssidMap = mutableMapOf<String, MutableList<BarEntry>>()

        // val values = mutableListOf<BarEntry>(
        // val dataSets = mutableListOf<IBarDataSet>()

        wifiScanData.forEachIndexed { index, networks ->
            val channel = index + 1
            networks.forEach { network ->
                val ssid = network.ssid
                val rssi = (network.rssi).toFloat()

                if (!ssidMap.containsKey(ssid)) {
                    ssidMap[ssid] = mutableListOf()
                }

                ssidMap[ssid]!!.add(BarEntry(channel.toFloat(), rssi))

            }
        }

        val dataSets = mutableListOf<IBarDataSet>()
        ssidMap.forEach { (ssid, barEntries) ->
            val dataSet = BarDataSet(barEntries, ssid)
            dataSet.color = getRandomColor()
            dataSets.add(dataSet)
        }

        chart1.data = BarData(dataSets)
        //  (number data sets) * (bar space + bar width) + group space = 1.0

        chart1.data.barWidth = 0.05f

        // chart1.groupBars(1f, 0.6f, 0.01f)
        chart1.data.groupBarsWithEmptySpaces(ssidMap.size)
        chart1.notifyDataSetChanged()
        chart1.setPinchZoom(false)
        chart1.setFitBars(true)
        chart1.description.isEnabled = false

        val xvals = List(11) {
            "${it + 1}"
        }

        chart1.xAxis.apply {
            setAvoidFirstLastClipping(true)
            // setCenterAxisLabels(true)
            setDrawGridLines(false)
            granularity = 1f
            // axisMaximum = 0 + chart1.barData.getGroupWidth(1f, 0.01f) * 11
            // axisMaximum = 0 + barChart.getBarData().getGroupWidth(groupSpace, barSpace) * groupCount
            labelCount = 11
            axisMinimum = 0f
            axisMaximum = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float, axis: AxisBase?): String {
                    return value.toInt().toString()
                }
            }
        }
        chart1.invalidate()
    }

    private fun getColors(): MutableList<Int> {
        val colors = IntArray(5)
        System.arraycopy(ColorTemplate.JOYFUL_COLORS, 0, colors, 0, 5)
        return colors.toMutableList()
    }
}

fun BarData.groupBarsWithEmptySpaces(
    barsCount: Int,
    @FloatRange(from = 0.0, to = 1.0) barSpaceFraction: Float = 0.1f
) {
    if (this.dataSets.size <= 1) {
        // Timber.tag("groupBarsWithEmptySpace").e("BarData needs to hold at least 2 BarDataSets to allow grouping.")
    } else {
        val allBarsWidth = this.barWidth * barsCount
        val barSpace = this.barWidth * barSpaceFraction
        val start = -(allBarsWidth + barSpace * barsCount - barSpace - this.barWidth) / 2
        val maxEntryCount = this.maxEntryCountSet.entryCount
        for (i in 0 until maxEntryCount) {
            var addition = start
            for (set in this.dataSets) {
                if (i < set.entryCount) {
                    val entry = set.getEntryForIndex(i)
                    if (entry != null) {
                        entry.x += addition
                    }
                }
                addition += this.barWidth
                addition += barSpace
            }
        }
        this.notifyDataChanged()
    }
}