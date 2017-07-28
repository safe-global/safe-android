package pm.gnosis.android.app.wallet.data.geth

import org.ethereum.geth.Account
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.BuildConfig
import pm.gnosis.android.app.wallet.data.PreferencesManager
import pm.gnosis.android.app.wallet.util.edit
import pm.gnosis.android.app.wallet.util.generateRandomString
import pm.gnosis.android.app.wallet.util.hexToByteArray
import pm.gnosis.android.app.wallet.util.test.TestRPC
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethAccountManager @Inject constructor(private val preferencesManager: PreferencesManager,
                                             private val keyStore: KeyStore) {
    init {
        if (keyStore.accounts.size() == 0L) {
            if (BuildConfig.DEBUG) {
                TestRPC.accounts.iterator().forEach({
                    val privateKey = it.value.first
                    val passphrase = it.value.second
                    keyStore.importECDSAKey(privateKey.hexToByteArray(), passphrase)
                })
            } else {
                keyStore.newAccount(getAccountPassphrase())
            }
            setActiveAccount(keyStore.accounts.get(0L).address.hex)
        }
    }

    fun getAccount(publicKey: String): Account? = getAccounts().find { it.address.hex == publicKey }

    fun getAccounts(): List<Account> {
        val accounts = ArrayList<Account>()
        for (i in 0..keyStore.accounts.size() - 1) {
            accounts += keyStore.accounts[i]
        }
        return accounts
    }

    fun setActiveAccount(publicKey: String): Boolean {
        getAccounts().find { it.address.hex == publicKey }?.let {
            preferencesManager.prefs.edit { putString(PreferencesManager.CURRENT_ACCOUNT_ADDRESS_KEY, it.address.hex) }
            return true
        }
        return false
    }

    fun getActiveAccount(): Account =
            getAccounts().find { it.address.hex == preferencesManager.prefs.getString(PreferencesManager.CURRENT_ACCOUNT_ADDRESS_KEY, null) }!! //we should always have an active account


    fun getAccountPassphrase(): String {
        var passphrase = preferencesManager.prefs.getString(PreferencesManager.PASSPHRASE_KEY, "")
        if (passphrase.isEmpty()) {
            preferencesManager.prefs.edit {
                passphrase = generateRandomString()
                putString(PreferencesManager.PASSPHRASE_KEY, passphrase)
            }
        }
        return passphrase
    }
}
