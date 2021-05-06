package io.gnosis.safe.ui.base.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.ScreenId
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.di.modules.ViewModule
import io.gnosis.safe.ui.settings.app.SettingsHandler
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var settingsHandler: SettingsHandler

    abstract fun screenId(): ScreenId?

    override fun onCreate(savedInstanceState: Bundle?) {
        HeimdallApplication[this].inject(this)
        super.onCreate(savedInstanceState)
        settingsHandler.allowScreenShots(window, settingsHandler.screenshotsAllowed)
        settingsHandler.allowTracking(this, settingsHandler.trackingAllowed)
        screenId()?.let {
            tracker.logScreen(it)
        }
        settingsHandler.applyNightMode(settingsHandler.nightMode)
        // in case app was updated in the background and restarted
        settingsHandler.updateUpdateInfo()
        settingsHandler.fetchRemoteConfig()
    }

    protected fun viewComponent(): ViewComponent =
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build()
}
