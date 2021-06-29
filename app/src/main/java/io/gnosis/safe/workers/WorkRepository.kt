package io.gnosis.safe.workers

import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkRepository
@Inject constructor(
    private val workManager: WorkManager
) {

    fun updateChainInfo() {
        val updateChainInfoRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UpdateChainInfoWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()
        workManager.enqueue(updateChainInfoRequest)
    }
}
