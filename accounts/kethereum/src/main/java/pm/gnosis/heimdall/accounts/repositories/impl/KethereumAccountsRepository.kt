package pm.gnosis.heimdall.accounts.repositories.impl

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okio.ByteString
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.crypto.KeyPair
import pm.gnosis.heimdall.accounts.data.db.AccountsDatabase
import pm.gnosis.heimdall.accounts.models.Account
import pm.gnosis.heimdall.accounts.models.Transaction
import pm.gnosis.heimdall.accounts.repositories.AccountsRepository
import pm.gnosis.heimdall.accounts.repositories.impl.models.db.AccountDb
import pm.gnosis.heimdall.accounts.utils.hash
import pm.gnosis.heimdall.accounts.utils.rlp
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.util.edit
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KethereumAccountsRepository @Inject internal constructor(private val accountsDatabase: AccountsDatabase,
                                                               private val preferencesManager: PreferencesManager) : AccountsRepository {
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
                val account = AccountDb()
                account.privateKey = privateKey.toHexString()
                accountsDatabase.accountsDao().insertAccount(account)
            }.subscribeOn(Schedulers.io())

    override fun saveAccountFromMnemonic(mnemonic: String, accountIndex: Long): Completable = Single.fromCallable {
        val hdNode = KeyGenerator().masterNode(ByteString.of(*Bip39.mnemonicToSeed(mnemonic)))
        hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM).deriveChild(accountIndex).keyPair
    }.flatMapCompletable {
        saveAccount(it.privKeyBytes ?: throw IllegalStateException("Private key must not be null"))
    }

    override fun saveMnemonic(mnemonic: String): Completable = Completable.fromCallable {
        preferencesManager.prefs.edit {
            //TODO: in the future this needs to be encrypted
            putString(PreferencesManager.MNEMONIC_KEY, mnemonic)
        }
    }
}
