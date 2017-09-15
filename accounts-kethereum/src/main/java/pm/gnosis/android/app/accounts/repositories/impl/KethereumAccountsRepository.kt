package pm.gnosis.android.app.accounts.repositories.impl

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.android.app.accounts.data.db.AccountsDb
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.accounts.utils.hash
import pm.gnosis.android.app.accounts.utils.rlp
import pm.gnosis.android.app.authenticator.util.asEthereumAddressString
import pm.gnosis.android.app.authenticator.util.hexAsBigInteger
import pm.gnosis.crypto.KeyPair
import pm.gnosis.utils.toHexString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KethereumAccountsRepository @Inject constructor(private val accountsDb: AccountsDb) : AccountsRepository {
    override fun loadActiveAccount(): Single<Account> {
        return keyPairFromActiveAccount()
                .map { it.pubKey.toHexString().asEthereumAddressString() }
                .map { Account(it) }
    }

    override fun signTransaction(transaction: Transaction): Single<String> {
        return keyPairFromActiveAccount()
                .map { transaction.rlp(it.sign(transaction.hash())).toHexString() }
    }

    private fun keyPairFromActiveAccount(): Single<KeyPair> {
        return accountsDb.accountsDao().observeAccounts()
                .subscribeOn(Schedulers.io())
                .map { it.privateKey?.hexAsBigInteger() }
                .map { KeyPair.fromPrivate(it) }
    }
}
