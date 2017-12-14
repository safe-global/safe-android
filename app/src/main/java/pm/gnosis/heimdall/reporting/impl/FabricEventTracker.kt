package pm.gnosis.heimdall.reporting.impl

import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import io.reactivex.Single
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FabricEventTracker @Inject constructor(
        private val fabricCore: FabricCore
) : EventTracker {

    private val answers = Answers.getInstance()

    override fun loadTrackingIdentifier() = Single.just(fabricCore.installId)!!

    override fun submit(event: Event) {
        return when (event) {
            is Event.ScreenView -> {
                answers.logContentView(ContentViewEvent().putContentName(event.id.name.toLowerCase()))
                trackCustomEvent(EVENT_NAME_SCREEN_VIEW, PARAM_NAME_ID to event.id.name.toLowerCase())
            }
            is Event.ButtonClick ->
                trackCustomEvent(EVENT_NAME_BUTTON_CLICK, PARAM_NAME_ID to event.id.name.toLowerCase())
            is Event.TabSelect ->
                trackCustomEvent(EVENT_NAME_TAB_SELECT, PARAM_NAME_ID to event.id.name.toLowerCase())
            is Event.SubmittedTransaction ->
                trackCustomEvent(EVENT_NAME_SUBMITTED_TRANSACTION)
        }
    }

    private fun trackCustomEvent(name: String, vararg params: Pair<String, String>) {
        Timber.i("$name>>>${params.joinToString()}")
        answers.logCustom(CustomEvent(name).apply {
            params.forEach { (key, value) ->
                putCustomAttribute(key, value)
            }
        })
    }

    companion object {
        private const val EVENT_NAME_SCREEN_VIEW = "screen_view"
        private const val EVENT_NAME_BUTTON_CLICK = "button_click"
        private const val EVENT_NAME_TAB_SELECT = "tab_select"
        private const val EVENT_NAME_SUBMITTED_TRANSACTION = "submitted_transaction"

        private const val PARAM_NAME_ID = "id"
    }
}