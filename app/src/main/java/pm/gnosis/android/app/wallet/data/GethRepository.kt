package pm.gnosis.android.app.wallet.data

import org.ethereum.geth.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethRepository @Inject constructor(private val accountManager: AccountManager,
                                         private val gethKeyStore: KeyStore) {
    fun getAccount() = accountManager.getAccount()
}