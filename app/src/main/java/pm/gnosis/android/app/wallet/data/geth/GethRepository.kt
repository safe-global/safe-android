package pm.gnosis.android.app.wallet.data.geth

import org.ethereum.geth.Address
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.util.hexToByteArray
import timber.log.Timber
import java.math.BigDecimal
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

        val newGasLimit = BigDecimal(gasLimit).multiply(BigDecimal(1.2)).toBigInteger()
        val transaction = Geth.newTransaction(nonce.toLong(), Address("0x${to.toString(16)}"),
                BigInt(amount?.toLong() ?: 0L), BigInt(400000L), BigInt(gasPrice.toLong()), dataBytes)


        Timber.d("Signing transaction: $transaction")
        val signed = gethKeyStore.signTxPassphrase(
                account, gethAccountManager.getAccountPassphrase(), transaction, BigInt(RinkebyParams.CHAIN_ID))
        val builder = StringBuilder()
        signed.encodeRLP().forEach { builder.append(String.format("%02x", it)) }
        return "0x$builder"
    }
}
