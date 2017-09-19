package pm.gnosis.android.app.authenticator.data

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.android.app.authenticator.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        const val GNOSIS_PREFS_NAME = "GnosisPrefs"
        const val FIRST_LAUNCH_KEY = "prefs.boolean.first_launch"
        const val PASSPHRASE_KEY = "prefs.string.passphrase"
        const val CURRENT_ACCOUNT_ADDRESS_KEY = "prefs.string.current_account"
        const val CURRENT_MULTISIG_ADDRESS = "prefs.string.current_multisig"
        const val MNEMONIC_KEY = "prefs.string.mnemonic"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(GNOSIS_PREFS_NAME, Context.MODE_PRIVATE)
}
