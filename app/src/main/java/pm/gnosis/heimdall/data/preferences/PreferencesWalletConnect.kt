package pm.gnosis.heimdall.data.preferences

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

class PreferencesWalletConnect(context: Context) {

    private val bridgePrefs: SharedPreferences
    private val sessionsPrefs: SharedPreferences

    init {
        bridgePrefs = context.getSharedPreferences(PREFERENCES_BRIDGE, Context.MODE_PRIVATE)
        sessionsPrefs = context.getSharedPreferences(PREFERENCES_SESSIONS, Context.MODE_PRIVATE)
    }

    var introDone: Boolean
        get() = bridgePrefs.getBoolean(KEY_INTRO_DONE, false)
        set(value) {
            bridgePrefs.edit { putBoolean(KEY_INTRO_DONE, value) }
        }

    fun safeForSession(sessionId: String): Solidity.Address? {
        return sessionsPrefs.getString(KEY_SESSION_SAFE_PREFIX + sessionId, null)?.asEthereumAddress()
    }

    fun saveSession(sessionId: String, safe: Solidity.Address) {
        sessionsPrefs.edit { putString(KEY_SESSION_SAFE_PREFIX + sessionId, safe.asEthereumAddressString()) }
    }

    fun removeSession(sessionId: String) {
        sessionsPrefs.edit { remove(KEY_SESSION_SAFE_PREFIX + sessionId) }
    }

    companion object {

        private const val PREFERENCES_SESSIONS = "preferences.wallet_connect_bridge_repository.sessions"
        private const val KEY_SESSION_SAFE_PREFIX = "session_safe_"

        private const val PREFERENCES_BRIDGE = "preferences.wallet_connect_bridge_repository.bridge"
        private const val KEY_INTRO_DONE = "wallet_connect_bridge_repository.boolean.intro_done"
    }
}