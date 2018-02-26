package pm.gnosis.heimdall.accounts.repositories.impls

import org.ethereum.geth.Account
import org.ethereum.geth.KeyStore
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.utils.edit
import pm.gnosis.utils.generateRandomString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethAccountManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val keyStore: KeyStore
) {

    fun getAccount(publicKey: String): Account? = getAccounts().find { it.address.hex == publicKey }

    fun getAccounts(): List<Account> {
        val accounts = ArrayList<Account>()
        for (i in 0 until keyStore.accounts.size()) {
            accounts += keyStore.accounts[i]
        }
        return accounts
    }

    fun setActiveAccount(publicKey: String): Boolean {
        getAccounts().find { it.address.hex == publicKey }?.let {
            preferencesManager.prefs.edit { putString(PreferencesManager.Companion.CURRENT_ACCOUNT_ADDRESS_KEY, it.address.hex) }
            return true
        }
        return false
    }

    fun getActiveAccount(): Account = getAccounts().first()

    fun getAccountPassphrase(): String {
        var passphrase = preferencesManager.prefs.getString(PreferencesManager.Companion.PASSPHRASE_KEY, "")
        if (passphrase.isEmpty()) {
            preferencesManager.prefs.edit {
                passphrase = generateRandomString()
                putString(PreferencesManager.Companion.PASSPHRASE_KEY, passphrase)
            }
        }
        return passphrase
    }
}
