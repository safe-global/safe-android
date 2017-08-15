package pm.gnosis.android.app.wallet.data.geth

import org.ethereum.geth.Address
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.util.asEthereumAddressString
import pm.gnosis.android.app.wallet.util.hexToByteArray
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethRepository @Inject constructor(private val gethAccountManager: GethAccountManager,
                                         private val gethKeyStore: KeyStore) {
    fun getAccount() = gethAccountManager.getActiveAccount()

    fun getAccounts() = gethAccountManager.getAccounts()

    fun setActiveAccount(publicKey: String) = gethAccountManager.setActiveAccount(publicKey)

    fun signTransaction(nonce: BigInteger, to: BigInteger, amount: BigInteger?, gasLimit: BigInteger,
                        gasPrice: BigInteger, data: String? = null): String {
        val account = getAccount()
        val dataBytes = data?.hexToByteArray()

        val transaction = Geth.newTransaction(nonce.toLong(), Address(to.asEthereumAddressString()),
                BigInt(amount?.toLong() ?: 0L), BigInt(400000L), BigInt(gasPrice.toLong()), dataBytes)


        Timber.d("Signing transaction: $transaction")
        val signed = gethKeyStore.signTxPassphrase(
                account, gethAccountManager.getAccountPassphrase(), transaction, BigInt(RinkebyParams.CHAIN_ID))
        Timber.d("Transaction signed: $signed")
        val builder = StringBuilder()
        signed.encodeRLP().forEach { builder.append(String.format("%02x", it)) }
        return "0x$builder"
    }
}
