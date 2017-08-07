package pm.gnosis.android.app.wallet.data

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.android.app.wallet.di.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {
    companion object {
        const val GNOSIS_PREFS_NAME = "GnosisPrefs"
        const val PASSPHRASE_KEY = "prefs.string.passphrase"
        const val CURRENT_ACCOUNT_ADDRESS_KEY = "prefs.string.current_account"
        const val CURRENT_MULTISIG_ADDRESS = "prefs.string.current_multisig"
    }

    val prefs: SharedPreferences = context.getSharedPreferences(GNOSIS_PREFS_NAME, Context.MODE_PRIVATE)
}
