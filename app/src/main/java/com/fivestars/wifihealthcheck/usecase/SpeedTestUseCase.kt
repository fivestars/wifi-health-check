package com.fivestars.wifihealthcheck.usecase

import android.os.AsyncTask
import android.util.Log
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.IRepeatListener
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.coroutines.Continuation

data class SpeedTestResults(
    val download: Double,
    val upload: Double
)

class SpeedTestUseCase {
    companion object {
        private const val DOWNLOAD_URL = "http://ashburn.va.speedtest.frontier.com:8080/speedtest/random4000x4000.jpg"
        private const val UPLOAD_URL = "http://ashburn.va.speedtest.frontier.com:8080/speedtest/upload.php"
    }

    suspend fun speedTest(): SpeedTestResults = coroutineScope {
        val download = withContext(Dispatchers.IO) { downLoadSpeedTest() }
        val upload = withContext(Dispatchers.IO) { uploadSpeedTest() }

        SpeedTestResults(download, upload)
    }

    private suspend fun downLoadSpeedTest(): Double = suspendCancellableCoroutine { cont ->
        val speedTestSocket = SpeedTestSocket()
        speedTestSocket.startDownloadRepeat(DOWNLOAD_URL, 20000, 2000, SpeedTestListener(cont))
    }

    private suspend fun uploadSpeedTest(): Double = suspendCancellableCoroutine { cont ->
        val speedTestSocket = SpeedTestSocket()
        speedTestSocket.startUploadRepeat(UPLOAD_URL, 20000, 2000, SpeedTestListener(cont))
    }
}

class SpeedTestListener(private val cont: Continuation<Double>) : IRepeatListener {
    val downloads = mutableListOf<BigDecimal>()

    override fun onCompletion(report: SpeedTestReport) {
        val numberOfTests = downloads.size
        val totalRate = downloads.reduce { acc, bigDecimal ->  acc + bigDecimal}
        val avg = totalRate.divide(BigDecimal.valueOf(numberOfTests.toDouble()), RoundingMode.HALF_UP)

        cont.resumeWith(Result.success(avg.toDouble()))

        Log.v("speedtest", "[completed] : ${report.transferRateBit}")
        Log.v("speedtest", "[completed] : $avg")
    }

    override fun onReport(report: SpeedTestReport) {
        var rate = report.transferRateBit
        val divisor = BigDecimal.valueOf(1000000)
        rate = rate.divide(divisor, RoundingMode.HALF_UP)
        downloads.add(rate)
    }
}