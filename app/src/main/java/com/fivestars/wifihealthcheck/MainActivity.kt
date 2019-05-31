package com.fivestars.wifihealthcheck

import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.IRepeatListener
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    val presenter = WifiHealthPresenter()
    val speedTest = SpeedTestTask()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter.startUp(this)

        val networkInfo = presenter.getNetworkInfo()
        val wifiInfo = presenter.wifiInfo()

        this.network_info.text = networkInfo.toString()
        this.wifi_info.text = wifiInfo.toString()

        speedTest.execute()

        getWifiPermission()

        progress_frame_layout.visibility = View.GONE
        results_layout.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.shutDown()
    }

    private fun getWifiPermission() {
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