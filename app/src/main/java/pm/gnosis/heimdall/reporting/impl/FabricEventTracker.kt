package pm.gnosis.heimdall.reporting.impl

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.google.firebase.analytics.FirebaseAnalytics
import io.reactivex.Single
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FabricEventTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fabricCore: FabricCore
) : EventTracker {

    private val answers = Answers.getInstance()
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context).apply {
        setUserId(fabricCore.installId)
    }

    override fun setCurrentScreenId(activity: Activity, id: ScreenId) {
        firebaseAnalytics.setCurrentScreen(activity, id.name.toLowerCase(), null)
    }

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
            is Event.SignedTransaction ->
                trackCustomEvent(EVENT_NAME_SIGNED_TRANSACTION)
        }
    }

    private fun trackCustomEvent(name: String, vararg params: Pair<String, String>) {
        if (BuildConfig.DEBUG) {
            Timber.i("$name>>>${params.joinToString()}")
        }
        firebaseAnalytics.logEvent(name, Bundle().apply {
            params.forEach { (key, value) ->
                putString(key, value)
            }
        })
        answers.logCustom(CustomEvent(name).apply {
            params.forEach { (key, value) ->
                putCustomAttribute(key, value)
            }
        })
    }

    companion object {
        private const val EVENT_NAME_SCREEN_VIEW = "gnosis_screen_view"
        private const val EVENT_NAME_BUTTON_CLICK = "button_click"
        private const val EVENT_NAME_TAB_SELECT = "tab_select"
        private const val EVENT_NAME_SUBMITTED_TRANSACTION = "submitted_transaction"
        private const val EVENT_NAME_SIGNED_TRANSACTION = "signed_transaction"

        private const val PARAM_NAME_ID = "id"
    }
}
