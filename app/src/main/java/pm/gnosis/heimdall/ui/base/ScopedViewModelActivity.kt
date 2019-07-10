package pm.gnosis.heimdall.ui.base

import android.os.Bundle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import pm.gnosis.heimdall.di.modules.ApplicationModule
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

abstract class ScopedViewModelActivity<VM: ViewModel>: ViewModelActivity<VM>(), CoroutineScope {

    @Inject
    lateinit var dispatchers: ApplicationModule.AppCoroutineDispatchers

    lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + dispatchers.main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = SupervisorJob()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}