package pm.gnosis.android.app.accounts.repositories.impl

import io.reactivex.Observable
import pm.gnosis.android.app.accounts.utils.hash
import pm.gnosis.android.app.accounts.utils.rlp
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.authenticator.data.PreferencesManager
import pm.gnosis.android.app.authenticator.util.edit
import pm.gnosis.android.app.core.BuildConfig
import pm.gnosis.crypto.KeyPair
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.toHexString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KethereumAccountsRepository @Inject constructor(
        preferencesManager: PreferencesManager
) : AccountsRepository {

    private val keyPair: KeyPair

    init {
        val accountKey = preferencesManager.prefs.getString(PREFERENCES_KEY_ACCOUNT, null)
        if (accountKey != null) {
            keyPair = KeyPair.fromPrivate(accountKey.hexStringToByteArray())
        } else if (BuildConfig.PRIVATE_TEST_NET) {
            keyPair = KeyPair.fromPrivate("8678adf78db8d1c8a40028795077b3463ca06a743ca37dfd28a5b4442c27b457".hexStringToByteArray())
        } else {
            // TODO: generate random account
            keyPair = KeyPair.fromPrivate("8678adf78db8d1c8a40028795077b3463ca06a743ca37dfd28a5b4442c27b457".hexStringToByteArray())
        }
        preferencesManager.prefs.edit { putString(PREFERENCES_KEY_ACCOUNT, keyPair.privKey.toString(16)) }
    }

    override fun loadActiveAccount(): io.reactivex.Observable<Account> {
        return Observable.just(keyPair).map {
            Account(it.address.toHexString())
        }
    }

    override fun signTransaction(transaction: Transaction): io.reactivex.Observable<String> {
        return Observable.fromCallable {
            transaction.rlp(keyPair.sign(transaction.hash())).toHexString()
        }
    }

    companion object {
        private const val PREFERENCES_KEY_ACCOUNT = "preferences_accounts.string.account"
    }
}