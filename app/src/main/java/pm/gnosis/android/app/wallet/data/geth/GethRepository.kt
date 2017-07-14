package pm.gnosis.android.app.wallet.data.geth

import org.ethereum.geth.Address
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethRepository @Inject constructor(private val gethAccountManager: GethAccountManager,
                                         private val gethKeyStore: KeyStore) {
    fun getAccount() = gethAccountManager.getAccount()

    fun signTransaction(nonce: BigInteger, to: BigInteger, amount: BigInteger = BigInteger.ZERO, gasLimit: BigInteger,
                        gasPrice: BigInteger, data: String = ""): String {
        val account = getAccount()

        val transaction = Geth.newTransaction(nonce.toLong(), Address("0x${to.toString(16)}"), BigInt(amount.toLong()),
                BigInt(gasLimit.toLong()), BigInt(gasPrice.toLong()), data.toByteArray())

        val signed = gethKeyStore.signTxPassphrase(
                account, gethAccountManager.getAccountPassphrase(), transaction, BigInt(RinkebyParams.CHAIN_ID))

        return signed.hash.toString()
    }
}
