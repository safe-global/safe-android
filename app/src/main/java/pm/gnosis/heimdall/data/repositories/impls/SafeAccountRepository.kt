package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okio.ByteString
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeInfoDb
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.data.db.AccountsDatabase
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.utils.asBigInteger
import javax.inject.Inject

class SafeAccountRepository @Inject constructor(
    accountsDatabase: AccountsDatabase,
    gnosisAuthenticatorDb: ApplicationDb,
    private val bip39: Bip39,
    private val cryptoHelper: CryptoHelper,
    private val encryptionManager: EncryptionManager
) : AccountsRepository {

    private val safeDao = gnosisAuthenticatorDb.gnosisSafeDao()
    private val accountsDao = accountsDatabase.accountsDao()

    override fun owners(): Single<List<AccountsRepository.SafeOwner>> =
        safeDao.loadSafeInfos()
            .map { it.map { a -> AccountsRepository.SafeOwner(a.ownerAddress, a.ownerPrivateKey) } }
            .flatMap { owners ->
                accountsDao.observeAccounts()
                    .map { owners + AccountsRepository.SafeOwner(it.address, it.privateKey) }
                    .onErrorReturnItem(owners)
            }

    override fun createOwner(): Single<AccountsRepository.SafeOwner> = Single.fromCallable {
        val seed = bip39.mnemonicToSeed(bip39.generateMnemonic(languageId = R.id.english))
        val hdNode = KeyGenerator.masterNode(ByteString.of(*seed))
        val key = hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM).deriveChild(0).keyPair
        val privateKey = key.privKeyBytes ?: throw IllegalStateException("Private key must not be null")
        val address = key.address.asBigInteger()
        AccountsRepository.SafeOwner(Solidity.Address(address), EncryptedByteArray.create(encryptionManager, privateKey))
    }.subscribeOn(Schedulers.io())

    override fun createOwnersFromPhrase(phrase: String, ids: List<Long>): Single<List<AccountsRepository.SafeOwner>> =
        Single.fromCallable {
            val mnemonicSeed = bip39.mnemonicToSeed(bip39.validateMnemonic(phrase))
            val hdNode = KeyGenerator.masterNode(ByteString.of(*mnemonicSeed))
            val masterKey = hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM)
            ids.map {
                val key = masterKey.deriveChild(it).keyPair
                val privateKey = key.privKeyBytes ?: throw IllegalStateException("Private key must not be null")
                val address = key.address.asBigInteger()
                AccountsRepository.SafeOwner(Solidity.Address(address), EncryptedByteArray.create(encryptionManager, privateKey))
            }
        }

    override fun saveOwner(safeAddress: Solidity.Address, safeOwner: AccountsRepository.SafeOwner, paymentToken: ERC20Token) =
        Completable.fromCallable {
            safeDao.insertSafeInfo(
                GnosisSafeInfoDb(
                    safeAddress,
                    safeOwner.address,
                    safeOwner.privateKey,
                    paymentToken.address,
                    paymentToken.symbol,
                    paymentToken.name,
                    paymentToken.decimals,
                    paymentToken.logoUrl
                )
            )
        }
            .subscribeOn(Schedulers.io())

    override fun signingOwner(safeAddress: Solidity.Address): Single<AccountsRepository.SafeOwner> =
        safeDao.loadSafeInfo(safeAddress)
            .map {
                AccountsRepository.SafeOwner(it.ownerAddress, it.ownerPrivateKey)
            }
            // use device account for legacy safes that don't have separate owner
            .onErrorResumeNext {
                accountsDao.observeAccounts().map { AccountsRepository.SafeOwner(it.address, it.privateKey) }
            }
            .subscribeOn(Schedulers.io())

    override fun sign(safeAddress: Solidity.Address, data: ByteArray): Single<Signature> =
        signingOwner(safeAddress)
            .flatMap { sign(it, data) }
            .subscribeOn(Schedulers.computation())


    override fun sign(safeOwner: AccountsRepository.SafeOwner, data: ByteArray): Single<Signature> =
        Single.fromCallable {
            cryptoHelper.sign(safeOwner.privateKey.value(encryptionManager), data)
        }.subscribeOn(Schedulers.computation())
}
