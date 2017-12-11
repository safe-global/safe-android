package pm.gnosis.heimdall.reporting

import io.reactivex.Single


interface EventTracker {
    fun submit(event: ScreenView)

    fun loadTrackingIdentifier(): Single<String>

    data class ScreenView(val screenName: String)
}