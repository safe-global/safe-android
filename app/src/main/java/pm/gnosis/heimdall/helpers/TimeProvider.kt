package pm.gnosis.heimdall.helpers

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton

interface TimeProvider {
    fun currentTimeMs(): Long
}

@Singleton
class LocalTimeProvider @Inject constructor(): TimeProvider {
    override fun currentTimeMs() = SystemClock.elapsedRealtime()
}