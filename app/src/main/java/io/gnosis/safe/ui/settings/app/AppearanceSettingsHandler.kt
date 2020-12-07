package io.gnosis.safe.ui.settings.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.NightMode
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearanceSettingsHandler @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    @NightMode
    var nightMode: Int
        get() = preferencesManager.prefs.getInt(KEY_NIGHT_MODE, MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) {
            preferencesManager.prefs.edit {
                putInt(KEY_NIGHT_MODE, value)
            }
        }

    fun applyNightMode(@NightMode nightMode: Int) {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    companion object {
        internal const val KEY_NIGHT_MODE = "prefs.string.appearance.night_mode"
    }
}
