package com.walterhblack.dovizwidget

import android.app.Application
import androidx.work.*
import com.walterhblack.dovizwidget.widget.RateRefreshWorker
import java.util.concurrent.TimeUnit

class DovizApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Android, uygulama kapalıyken de bu işi uygun bir zamanda çalıştırabilir.
        val request = PeriodicWorkRequestBuilder<RateRefreshWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("daily-rates", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
