package pm.gnosis.heimdall.reporting.impl

import com.crashlytics.android.Crashlytics
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.reporting.CrashTracker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FabricCrashTracker @Inject constructor(
        // We reference the core here to make sure that it is initialized
        @Suppress("unused") private val fabricCore: FabricCore
): CrashTracker {

    override fun init() {
        Timber.plant(Tree())
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    inner class Tree: Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            t?.let { Crashlytics.logException(t) }
            Crashlytics.log(priority, tag, message)
        }
    }
}