package pm.gnosis.android.app.accounts.repositories.impl

import io.reactivex.Observable
import org.ethereum.geth.Address
import org.ethereum.geth.BigInt
import org.ethereum.geth.Geth
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.authenticator.util.asEthereumAddressString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GethAccountsRepository @Inject constructor(
        private val gethAccountManager: GethAccountManager,
        private val gethKeyStore: KeyStore
) : AccountsRepository {
    override fun loadActiveAccount(): io.reactivex.Observable<Account> {
        return Observable.fromCallable {
            gethAccountManager.getActiveAccount()
        }.map {
            Account(it.address.hex)
        }
    }

    override fun signTransaction(transaction: Transaction): io.reactivex.Observable<String> {
        return Observable.fromCallable {
            val account = gethAccountManager.getActiveAccount()
            val tx = Geth.newTransaction(
                    transaction.nonce.toLong(),
                    Address(transaction.to.asEthereumAddressString()),
                    BigInt(transaction.value.toLong()),
                    BigInt(transaction.startGas.toLong()),
                    BigInt(transaction.gasPrice.toLong()),
                    transaction.data)


            val signed = gethKeyStore.signTxPassphrase(
                    account, gethAccountManager.getAccountPassphrase(), tx, BigInt(transaction.chainCode.toLong()))

            val builder = StringBuilder()
            signed.encodeRLP().forEach { builder.append(String.format("%02x", it)) }
            "0x$builder"
        }
    }
}