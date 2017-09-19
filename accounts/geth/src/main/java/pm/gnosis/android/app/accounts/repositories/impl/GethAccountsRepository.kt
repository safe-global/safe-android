package pm.gnosis.android.app.accounts.repositories.impl

import io.reactivex.Completable
import io.reactivex.Single
import org.ethereum.geth.Address
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.authenticator.util.asEthereumAddressString
import pm.gnosis.utils.toHexString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethAccountsRepository @Inject constructor(
        private val gethAccountManager: GethAccountManager,
        private val gethKeyStore: KeyStore
) : AccountsRepository {
    override fun loadActiveAccount(): Single<Account> {
        return Single.fromCallable {
            gethAccountManager.getActiveAccount()
        }.map {
            Account(it.address.hex)
        }
    }

    override fun signTransaction(transaction: Transaction): Single<String> {
        return Single.fromCallable {
            val account = gethAccountManager.getActiveAccount()
            val tx = Geth.newTransaction(
                    transaction.nonce.toLong(),
                    Address(transaction.to.asEthereumAddressString()),
                    BigInt(transaction.value.toLong()),
                    BigInt(transaction.adjustedStartGas.toLong()),
                    BigInt(transaction.gasPrice.toLong()),
                    transaction.data)


            val signed = gethKeyStore.signTxPassphrase(
                    account, gethAccountManager.getAccountPassphrase(), tx, BigInt(transaction.chainId.toLong()))

            signed.encodeRLP().toHexString()
        }
    }

    override fun saveAccount(privateKey: ByteArray): Completable {
        TODO("not implemented")
    }
}
