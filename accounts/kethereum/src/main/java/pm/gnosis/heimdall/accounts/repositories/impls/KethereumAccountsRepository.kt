package pm.gnosis.heimdall.accounts.repositories.impls

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okio.ByteString
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.crypto.KeyPair
import pm.gnosis.heimdall.accounts.base.exceptions.InvalidTransactionParams
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.models.Signature
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.accounts.data.db.AccountsDatabase
import pm.gnosis.heimdall.accounts.repositories.impls.models.db.AccountDb
import pm.gnosis.heimdall.accounts.utils.hash
import pm.gnosis.heimdall.accounts.utils.rlp
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.common.utils.edit
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.db.EncryptedByteArray
import pm.gnosis.heimdall.security.db.EncryptedString
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.models.Transaction
import pm.gnosis.utils.addHexPrefix
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.isValidEthereumAddress
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KethereumAccountsRepository @Inject internal constructor(
        private val accountsDatabase: AccountsDatabase,
        private val encryptionManager: EncryptionManager,
        private val preferencesManager: PreferencesManager,
        private val bip39: Bip39
) : AccountsRepository {

    private val encryptedStringConverter = EncryptedString.Converter()

    override fun loadActiveAccount(): Single<Account> {
        return accountsDatabase.accountsDao().observeAccounts()
                .subscribeOn(Schedulers.io())
                .map { Account(it.address) }
    }

    override fun signTransaction(transaction: Transaction): Single<String> {
        if (!transaction.signable()) return Single.error(InvalidTransactionParams())
        return keyPairFromActiveAccount()
                .map { transaction.rlp(it.sign(transaction.hash())).toHexString().addHexPrefix() }
    }

    override fun sign(data: ByteArray): Single<Signature> {
        return keyPairFromActiveAccount()
                .map { it.sign(data).let { Signature(it.r, it.s, it.v) } }
    }

    override fun recover(data: ByteArray, signature: Signature): Single<BigInteger> =
            Single.fromCallable {
                KeyPair.signatureToKey(data, signature.v, signature.r, signature.s).address.asBigInteger()
            }

    private fun keyPairFromActiveAccount(): Single<KeyPair> {
        return accountsDatabase.accountsDao().observeAccounts()
                .subscribeOn(Schedulers.io())
                .map { it.privateKey.value(encryptionManager).asBigInteger() }
                .map { KeyPair.fromPrivate(it) }
    }

    override fun saveAccountFromMnemonic(mnemonic: String, accountIndex: Long): Completable = Completable.fromAction {
        val hdNode = KeyGenerator().masterNode(ByteString.of(*bip39.mnemonicToSeed(mnemonic)))
        val key = hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM).deriveChild(accountIndex).keyPair
        val privateKey = key.privKeyBytes ?: throw IllegalStateException("Private key must not be null")
        val address = key.address.asBigInteger().apply {
            isValidEthereumAddress()
        }
        val account = AccountDb(EncryptedByteArray.create(encryptionManager, privateKey), address)
        accountsDatabase.accountsDao().insertAccount(account)
    }.subscribeOn(Schedulers.io())

    override fun saveMnemonic(mnemonic: String): Completable = Completable.fromCallable {
        preferencesManager.prefs.edit {
            val encryptedString = EncryptedString.create(encryptionManager, mnemonic)
            putString(PreferencesManager.MNEMONIC_KEY, encryptedStringConverter.toStorage(encryptedString))
        }
    }.subscribeOn(Schedulers.computation())

    override fun loadMnemonic(): Single<String> = Single.fromCallable {
        val encryptedMnemonic = preferencesManager.prefs.getString(PreferencesManager.MNEMONIC_KEY, "")
        encryptedStringConverter.fromStorage(encryptedMnemonic).value(encryptionManager)
    }.subscribeOn(Schedulers.computation())

    override fun generateMnemonic(): Single<String> = Single.fromCallable { bip39.generateMnemonic() }

    override fun validateMnemonic(mnemonic: String): Single<String> = Single.fromCallable { bip39.validateMnemonic(mnemonic) }
}
