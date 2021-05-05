package io.gnosis.safe.ui.updates

import androidx.lifecycle.ViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import javax.inject.Inject

class UpdatesViewModel
@Inject constructor(
    private val settingsHandler: SettingsHandler
) : ViewModel() {

    fun setUpdateShownForVersionFlag() {
        settingsHandler.updateNewestVersionShown = true
    }

    fun updateUpdateInfo() {
        settingsHandler.updateUpdateInfo()
    }
}
