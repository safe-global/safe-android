package io.gnosis.safe.ui.settings.app

import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenshotAuthorizer @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    fun setAllowScreenshots(value: Boolean) {
        preferencesManager.prefs.edit {
            putBoolean(ALLOW_SCREENSHOTS, value)
        }
    }

    fun getAllowScreenshots(): Boolean {

        val result = preferencesManager.prefs.getBoolean(ALLOW_SCREENSHOTS, false)
        return result
    }

    companion object {
        const val ALLOW_SCREENSHOTS = "prefs.boolean.allow_screenshots"
    }
}
