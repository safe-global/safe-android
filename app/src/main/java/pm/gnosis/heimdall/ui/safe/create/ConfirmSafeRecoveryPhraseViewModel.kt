package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationParams
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.accounts.utils.hash
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.utils.ethereum.getDeployAddressFromNonce
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.utf8String
import pm.gnosis.utils.words
import java.math.BigInteger
import java.security.SecureRandom
import javax.inject.Inject

class ConfirmSafeRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39,
    private val encryptionManager: EncryptionManager,
    private val relayServiceApi: RelayServiceApi,
    private val gnosisSafeRepository: GnosisSafeRepository
) : ConfirmSafeRecoveryPhraseContract() {
    private lateinit var mnemonic: String

    private lateinit var chromeExtensionAddress: Solidity.Address

    private val secureRandom = SecureRandom()

    override fun setup(encryptedMnemonic: String, chromeExtensionAddress: Solidity.Address): Single<List<String>> =
        Completable.fromAction {
            val mnemonic = encryptionManager.decrypt(EncryptionManager.CryptoData.fromString(encryptedMnemonic)).utf8String()
            if (mnemonic.words().size != 12) throw IllegalStateException("Invalid mnemonic")
            this.mnemonic = mnemonic
            this.chromeExtensionAddress = chromeExtensionAddress
        }
            .andThen(loadWordsToDisplay())
            .subscribeOn(Schedulers.io())

    private fun loadWordsToDisplay(): Single<List<String>> =
        Single.fromCallable {
            mnemonic.words().sorted()
        }.subscribeOn(Schedulers.computation())

    override fun isCorrectSequence(words: List<String>): Single<Result<Boolean>> =
        Single.fromCallable {
            words.joinToString(" ") == mnemonic
        }.subscribeOn(Schedulers.computation()).mapToResult()

    override fun createSafe(): Single<Result<Solidity.Address>> =
        loadSafeOwners()
            .map { RelaySafeCreationParams(owners = it, threshold = 2, s = BigInteger(252, secureRandom)) }
            .flatMap { request -> relayServiceApi.safeCreation(request).map { request to it } }
            // Check returned s parameter
            .map { (request, response) ->
                if (request.s != response.signature.s) throw IllegalStateException("Client provided parameter s does not match the one returned by the service")
                response
            }
            // TODO check bytecode. (sync with backend on the version being used)
            .map { response ->
                response to Transaction(
                    address = Solidity.Address(BigInteger.ZERO),
                    gas = response.tx.gas,
                    gasPrice = response.tx.gasPrice,
                    data = response.tx.data,
                    nonce = response.tx.nonce
                )
            }
            // Check returned safe address
            .flatMap { (response, tx) ->
                checkGeneratedSafeAddress(tx.hash(), response.signature, response.safe).andThen(
                    Single.just(response to tx)
                )
            }
            .flatMap { (response, tx) ->
                val transactionHash =
                    tx.hash(ECDSASignature(response.signature.r, response.signature.s).apply { v = response.signature.v.toByte() }).asBigInteger()
                gnosisSafeRepository.addPendingSafe(response.safe, transactionHash, null, response.payment)
                    .andThen(Single.just(response.safe))
            }
            .mapToResult()
            .subscribeOn(Schedulers.io())

    private fun checkGeneratedSafeAddress(transactionHash: ByteArray, signature: ServiceSignature, safeAddress: Solidity.Address) =
        accountsRepository.recover(transactionHash, signature.toSignature())
            .flatMapCompletable {
                Completable.fromCallable {
                    if (getDeployAddressFromNonce(
                            it,
                            BigInteger.ZERO
                        ).value != safeAddress.value
                    ) throw IllegalStateException("Address returned does not match address from signature")
                }
            }
            .subscribeOn(Schedulers.computation())

    private fun loadSafeOwners() =
        accountsRepository.loadActiveAccount().map { it.address }
            .flatMap { deviceAddress ->
                val mnemonicSeed = bip39.mnemonicToSeed(mnemonic)
                Single.zip(
                    accountsRepository.accountFromMnemonicSeed(mnemonicSeed, 0).map { it.first },
                    accountsRepository.accountFromMnemonicSeed(mnemonicSeed, 1).map { it.first },
                    BiFunction<Solidity.Address, Solidity.Address, List<Solidity.Address>> { recoveryAccount1, recoveryAccount2 ->
                        listOf(
                            deviceAddress,
                            chromeExtensionAddress,
                            recoveryAccount1,
                            recoveryAccount2
                        )
                    }
                )
            }
}
