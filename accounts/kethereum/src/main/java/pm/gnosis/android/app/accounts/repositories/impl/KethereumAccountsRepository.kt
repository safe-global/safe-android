package pm.gnosis.android.app.accounts.repositories.impl

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.android.app.accounts.data.db.AccountsDatabase
import pm.gnosis.android.app.accounts.models.Account
import pm.gnosis.android.app.accounts.models.Transaction
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.accounts.repositories.impl.models.db.AccountDb
import pm.gnosis.android.app.accounts.utils.hash
import pm.gnosis.android.app.accounts.utils.rlp
import pm.gnosis.android.app.authenticator.util.asEthereumAddressString
import pm.gnosis.android.app.authenticator.util.hexAsBigInteger
import pm.gnosis.crypto.KeyPair
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KethereumAccountsRepository @Inject internal constructor(private val accountsDatabase: AccountsDatabase) : AccountsRepository {
    override fun loadActiveAccount(): Single<Account> {
        return keyPairFromActiveAccount()
                .map { it.address.toHexString().asEthereumAddressString() }
                .map { Account(it) }
    }

    override fun signTransaction(transaction: Transaction): Single<String> {
        return keyPairFromActiveAccount()
                .map { transaction.rlp(it.sign(transaction.hash())).toHexString() }
    }

    private fun keyPairFromActiveAccount(): Single<KeyPair> {
        return accountsDatabase.accountsDao().observeAccounts()
                .subscribeOn(Schedulers.io())
                .map { it.privateKey?.hexAsBigInteger() }
                .map { KeyPair.fromPrivate(it) }
    }

    override fun saveAccount(privateKey: ByteArray): Completable =
            Completable.fromCallable {
                Timber.d("AAAAASDASDJASDIASJDISD")
                val account = AccountDb()
                account.privateKey = privateKey.toHexString()
                Timber.d("${accountsDatabase.accountsDao().insertAccount(account)}")
            }.subscribeOn(Schedulers.io())
}
