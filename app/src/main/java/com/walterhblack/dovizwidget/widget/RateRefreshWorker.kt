package com.walterhblack.dovizwidget.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.walterhblack.dovizwidget.data.RateRepository
import kotlinx.coroutines.CancellationException

class RateRefreshWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        RateRepository(applicationContext).refresh()
        publishWidgetState(applicationContext)
        Result.success()
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { if (runAttemptCount < 3) Result.retry() else Result.failure() }
}
