package pm.gnosis.heimdall.reporting.impl

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import io.reactivex.Single
import pm.gnosis.heimdall.reporting.EventTracker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FabricEventTracker @Inject constructor(
        private val fabricCore: FabricCore
) : EventTracker {
    override fun loadTrackingIdentifier() = Single.just(fabricCore.installId)!!

    override fun submit(event: EventTracker.ScreenView) {
        Answers.getInstance().logContentView(ContentViewEvent().putContentName(event.screenName))
    }
}