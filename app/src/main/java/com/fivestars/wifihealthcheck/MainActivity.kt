package com.fivestars.wifihealthcheck

import android.Manifest
import android.content.pm.PackageManager
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.IRepeatListener
import kotlinx.android.synthetic.main.activity_main.*
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : AppCompatActivity() {

    val presenter = SystemStatsPresenter()
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


            // val url = "http://rbx-fr.verelox.com/speedtest/random2000x2000.jpg"
            // val url = "http://laspeed.wiline.com:8080/speedtest/random2000x2000.jpg"
            val url = "https://ashburn02.speedtest.windstream.net:8080/download?nocache=bce25e94-4178-43f4-939d-dd9587321aad&size=250"
            // val url = "http://ashburn02.speedtest.windstream.net:8080/random2000x2000.jpg"
            // val url = "http://ipv4.ikoula.testdebit.info/1M.iso"
            //val url = "http://127.0.0.1/mini/speedtest/random2000x2000.jpg"
            // speedTestSocket.startDownload(url)
            speedTestSocket.startDownloadRepeat(url, 20000, 2000, object : IRepeatListener {
                override fun onCompletion(report: SpeedTestReport) {
                    /*
                    Log.v("speedtest", "[COMPLETED] rate in octet/s : " + report.transferRateOctet)
                    Log.v("speedtest", "[COMPLETED] rate in bit/s   : " + report.transferRateBit)
                    */

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
                    /*
                    Log.v("test", "DOWNLOAD SPEED, R: $rateString")

                    Log.v("speedtest", "[PROGRESS] progress : ${report.progressPercent}%")
                    Log.v("speedtest", "[PROGRESS] rate in octet/s : " + report.transferRateOctet)
                    Log.v("speedtest", "[PROGRESS] rate in bit/s   : " + report.transferRateBit)*/
                }
            })

            return null
        }
    }
}