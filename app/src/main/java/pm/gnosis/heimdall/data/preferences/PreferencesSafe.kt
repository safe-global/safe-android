package pm.gnosis.heimdall.data.preferences

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.util.*

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

    var lastSyncedData: String
        get() = prefs.getString(KEY_LAST_SYNC_PUSH_INFO, "")
        set(value) {
            prefs.edit { putString(KEY_LAST_SYNC_PUSH_INFO, value) }
        }

    var installId: String
        get() = prefs.getString(KEY_INSTALL_ID, null) ?: generateAndStoreInstallId()
        set(value) {
            prefs.edit { putString(KEY_INSTALL_ID, value) }
        }


    private fun generateAndStoreInstallId(): String {
        val id = UUID.randomUUID().toString()
        installId = id
        return id
    }

    companion object {
        const val PREFERENCES_SAFE = "GnosisPrefs"
        private const val KEY_SELECTED_SAFE = "safe_main.string.selected_safe"
        private const val KEY_LAST_SYNC_PUSH_INFO = "prefs.string.accounttoken"
        private const val KEY_INSTALL_ID = "fabric_core.string.install_id"
    }
}