package pm.gnosis.heimdall.data.preferences

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddressString

class PreferencesWalletConnect(context: Context) {

    private val bridgePreferences: SharedPreferences
    private val sessionsPreferences: SharedPreferences

    init {
        bridgePreferences = context.getSharedPreferences(PREFERENCES_BRIDGE, Context.MODE_PRIVATE)
        sessionsPreferences = context.getSharedPreferences(PREFERENCES_SESSIONS, Context.MODE_PRIVATE)
    }

    var keyIntroDone: Boolean
        get() = bridgePreferences.getBoolean(KEY_INTRO_DONE, false)
        set(value) {
            bridgePreferences.edit { putBoolean(KEY_INTRO_DONE, value) }
        }

    fun safeForSession(sessionId: String): Solidity.Address? {
        return sessionsPreferences.getString(KEY_SESSION_SAFE_PREFIX + sessionId, null)?.asEthereumAddress()
    }

    fun saveSession(sessionId: String, safe: Solidity.Address) {
        sessionsPreferences.edit { putString(KEY_SESSION_SAFE_PREFIX + sessionId, safe.asEthereumAddressString()) }
    }

    fun removeSession(sessionId: String) {
        sessionsPreferences.edit { remove(KEY_SESSION_SAFE_PREFIX + sessionId) }
    }

    companion object {

        private const val PREFERENCES_SESSIONS = "preferences.wallet_connect_bridge_repository.sessions"
        private const val KEY_SESSION_SAFE_PREFIX = "session_safe_"

        private const val PREFERENCES_BRIDGE = "preferences.wallet_connect_bridge_repository.bridge"
        private const val KEY_INTRO_DONE = "wallet_connect_bridge_repository.boolean.intro_done"
    }
}