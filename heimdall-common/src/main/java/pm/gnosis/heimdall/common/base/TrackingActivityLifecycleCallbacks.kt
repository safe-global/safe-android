package pm.gnosis.heimdall.common.base

import android.app.Activity
import android.app.Application
import android.os.Bundle


abstract class TrackingActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    private var activeActivityCount = 0

    abstract fun active()

    abstract fun inactive()

    override fun onActivityPaused(activity: Activity?) {
    }

    override fun onActivityResumed(activity: Activity?) {
    }

    override fun onActivityStarted(activity: Activity?) {
        if (activeActivityCount == 0) {
            // We had no active activities, so we just became active
            active()
        }
        activeActivityCount++
    }

    override fun onActivityDestroyed(activity: Activity?) {
    }

    override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity?) {
        activeActivityCount--
        if (activeActivityCount == 0) {
            // We have no more active activities, so we are inactive
            inactive()
        }
    }

    override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {
    }
}
