package io.gnosis.safe.ui.settings.app

import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacySettingsHandler @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    var screenshotsAllowed: Boolean
        get() = preferencesManager.prefs.getBoolean(KEY_ALLOW_SCREENSHOTS, false)
        set(value) {
            preferencesManager.prefs.edit {
                putBoolean(KEY_ALLOW_SCREENSHOTS, value)
            }
        }

    companion object {
       internal const val KEY_ALLOW_SCREENSHOTS = "prefs.boolean.allow_screenshots"
    }
}
