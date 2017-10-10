package pm.gnosis.heimdall.common

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.heimdall.common.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        const val GNOSIS_PREFS_NAME = "GnosisPrefs"
        const val FINISHED_TOKENS_SETUP = "prefs.boolean.finished_tokens_setup"
        const val PASSPHRASE_KEY = "prefs.string.passphrase"
        const val CURRENT_ACCOUNT_ADDRESS_KEY = "prefs.string.current_account"
        const val MNEMONIC_KEY = "prefs.string.mnemonic"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(GNOSIS_PREFS_NAME, Context.MODE_PRIVATE)
}
