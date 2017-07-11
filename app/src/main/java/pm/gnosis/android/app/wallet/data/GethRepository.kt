package pm.gnosis.android.app.wallet.data

import org.ethereum.geth.BigInt
import org.ethereum.geth.KeyStore
import org.ethereum.geth.Transaction
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethRepository @Inject constructor(private val accountManager: AccountManager,
                                         private val gethKeyStore: KeyStore) {
    fun getAccount() = accountManager.getAccount()

    fun sendRawTransaction() {
        val account = getAccount()
        val tx = Transaction(
                1, account.address,
                BigInt(0), BigInt(0), BigInt(1), null) // Random empty transaction
        val signed = gethKeyStore.signTxPassphrase(account, accountManager.getAccountPassphrase(), tx, BigInt(3))
        Timber.d(signed.toString())
    }
}