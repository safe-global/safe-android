package pm.gnosis.android.app.wallet.data

import org.ethereum.geth.Address
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.data.remote.RinkebyParams
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethRepository @Inject constructor(private val gethAccountManager: GethAccountManager,
                                         private val gethKeyStore: KeyStore) {
    fun getAccount() = gethAccountManager.getAccount()

    fun sendRawTransaction(nonce: Long, to: String, amount: BigInteger, gasLimit: BigInteger,
                           gasPrice: BigInteger, data: String) {
        val account = getAccount()

        val transaction = Geth.newTransaction(nonce, Address(to), BigInt(amount.toLong()),
                BigInt(gasLimit.toLong()), BigInt(gasPrice.toLong()), data.toByteArray())

        val signed = gethKeyStore.signTxPassphrase(
                account, gethAccountManager.getAccountPassphrase(), transaction, BigInt(RinkebyParams.CHAIN_ID))

        Timber.d(signed.hash.toString())
    }
}
