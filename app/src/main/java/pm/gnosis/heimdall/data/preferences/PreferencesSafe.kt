package pm.gnosis.heimdall.data.preferences

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class PreferencesSafe(context: Context) {

    private val prefs: SharedPreferences

    init {
        prefs = context.getSharedPreferences(PREFERENCES_SAFE, Context.MODE_PRIVATE)
    }

    var selectedSafe: Solidity.Address?
        get() = prefs.getString(KEY_SELECTED_SAFE, null)?.asEthereumAddress()
        set(value) {
            prefs.edit { putString(KEY_SELECTED_SAFE, value?.asEthereumAddressString()) }
        }

    companion object {
        const val PREFERENCES_SAFE = "GnosisPrefs"
        private const val KEY_SELECTED_SAFE = "safe_main.string.selected_safe"
    }
}