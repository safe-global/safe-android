package pm.gnosis.heimdall.ui.safe.helpers

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.KeyPair
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.Bip39ValidationResult
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.nullOnThrow
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

private typealias SigningAccounts = Pair<AccountsRepository.SafeOwner, AccountsRepository.SafeOwner>

private data class RecoverySafeData(
    val safeOwner: AccountsRepository.SafeOwner,
    val signingAccounts: SigningAccounts,
    val safeTransaction: SafeTransaction
)

interface RecoverSafeOwnersHelper {
    fun process(
        input: InputRecoveryPhraseContract.Input,
        safeAddress: Solidity.Address,
        extensionAddress: Solidity.Address?,
        safeOwner: AccountsRepository.SafeOwner?
    ):
            Observable<InputRecoveryPhraseContract.ViewUpdate>

    /**
     * Creates a SafeTransaction that will insert the addresses in addressesToSwapIn without removing the addressesToKeep.
     * The total number of owners of the safe will not change ie.: for each owner swapped in, another one will be swapped out
     *
     * @param addressesToSwapIn
     */
    fun buildRecoverTransaction(
        safeInfo: SafeInfo,
        addressesToKeep: Set<Solidity.Address>,
        addressesToSwapIn: Set<Solidity.Address>
    ): SafeTransaction
}

@Singleton
class DefaultRecoverSafeOwnersHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountsRepository: AccountsRepository,
    private val executionRepository: TransactionExecutionRepository,
    private val safeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository
) : RecoverSafeOwnersHelper {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun process(
        input: InputRecoveryPhraseContract.Input,
        safeAddress: Solidity.Address,
        extensionAddress: Solidity.Address?,
        safeOwner: AccountsRepository.SafeOwner?
    ): Observable<InputRecoveryPhraseContract.ViewUpdate> =
        input.retry
            .subscribeOn(AndroidSchedulers.mainThread())
            .startWith(Unit)
            .switchMap {
                safeRepository.loadInfo(safeAddress)
                    .onErrorResumeNext { error: Throwable -> errorHandler.observable(error) }
                    .mapToResult()
            }
            .flatMap {
                when (it) {
                    is ErrorResult -> Observable.just(
                        InputRecoveryPhraseContract.ViewUpdate.SafeInfoError(
                            it.error
                        )
                    )
                    is DataResult ->
                        // We have the safe info so lets show the mnemonic input
                        Observable.just<InputRecoveryPhraseContract.ViewUpdate>(InputRecoveryPhraseContract.ViewUpdate.InputMnemonic)
                            .concatWith(processRecoveryPhrase(input, it.data, extensionAddress, safeOwner))
                }
            }

    private fun processRecoveryPhrase(
        input: InputRecoveryPhraseContract.Input,
        safeInfo: SafeInfo,
        extensionAddress: Solidity.Address?,
        safeOwner: AccountsRepository.SafeOwner?
    ): Observable<InputRecoveryPhraseContract.ViewUpdate> =
        input.phrase
            .flatMapSingle {
                accountsRepository.createOwnersFromPhrase(it.toString(), listOf(0, 1))
                    .map { accounts ->
                        val account0 = accounts[0]
                        val account1 = accounts[1]
                        val expectedOwnerCount = extensionAddress?.let { 4 } ?: 3
                        if (safeInfo.owners.size != expectedOwnerCount) throw IllegalStateException()
                        // Check that the accounts generated by the mnemonic are owners
                        if (!safeInfo.owners.contains(account0.address) ||
                            !safeInfo.owners.contains(account1.address)
                        ) throw IllegalArgumentException()

                        SigningAccounts(account0, account1)
                    }
                    .flatMap { signingAccounts ->
                        // If no safeOwner is provided we try to pull the one for the specified Safe
                        (safeOwner?.let { Single.just(safeOwner) } ?: accountsRepository.signingOwner(safeInfo.address))
                            .map { owner -> owner to signingAccounts }
                    }
                    .map { (safeOwner, signingAccounts) ->

                        RecoverySafeData(
                            safeOwner,
                            signingAccounts,
                            buildRecoverTransaction(
                                safeInfo,
                                addressesToKeep = setOf(signingAccounts.first.address, signingAccounts.second.address),
                                addressesToSwapIn = listOfNotNull(safeOwner.address, extensionAddress).toSet()
                            )
                        )
                    }
                    .mapToResult()
            }
            .flatMap {
                when (it) {
                    // If errors are thrown while parsing the mnemonic or building the data, we should map them here
                    is ErrorResult -> Observable.just(
                        when (it.error) {
                            is Bip39ValidationResult -> InputRecoveryPhraseContract.ViewUpdate.InvalidMnemonic
                            is NoNeedToRecoverSafeException -> InputRecoveryPhraseContract.ViewUpdate.NoRecoveryNecessary(safeInfo.address)
                            else -> InputRecoveryPhraseContract.ViewUpdate.WrongMnemonic
                        }
                    )
                    // We successfully parsed the mnemonic and build the data, now we can show the create button and if pressed pull the data
                    is DataResult -> {
                        val (owner, signingAccounts, transaction) = it.data
                        input.create
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .switchMapSingle {
                                prepareTransaction(safeInfo, transaction, signingAccounts, owner)
                            }.startWith(InputRecoveryPhraseContract.ViewUpdate.ValidMnemonic)
                    }
                }
            }

    override fun buildRecoverTransaction(
        safeInfo: SafeInfo,
        addressesToKeep: Set<Solidity.Address>,
        addressesToSwapIn: Set<Solidity.Address>
    ): SafeTransaction =
        buildTransaction(safeInfo, buildPayloads(safeInfo, addressesToKeep, addressesToSwapIn))

    private fun buildPayloads(
        safeInfo: SafeInfo,
        addressesToKeep: Set<Solidity.Address>,
        addressesToAdd: Set<Solidity.Address>
    ): List<String> {
        val payloads = mutableListOf<String>()
        val remainingNewAddresses = addressesToAdd.toMutableList()
        for (i in safeInfo.owners.size - 1 downTo 0) {
            val safeOwner = safeInfo.owners[i]
            if (addressesToKeep.contains(safeOwner)) continue
            if (remainingNewAddresses.remove(safeOwner)) continue

            var newOwner: Solidity.Address? = null
            while (!remainingNewAddresses.isEmpty()) {
                newOwner = remainingNewAddresses.removeAt(remainingNewAddresses.lastIndex)
                if (!safeInfo.owners.contains(newOwner)) break
                newOwner = null
            }
            newOwner ?: break

            val pointerAddress = nullOnThrow { safeInfo.owners[i - 1] } ?: SENTINEL
            payloads += GnosisSafe.SwapOwner.encode(
                prevOwner = pointerAddress,
                oldOwner = safeOwner,
                newOwner = newOwner
            )
        }
        if (remainingNewAddresses.isNotEmpty()) throw IllegalStateException("Couldn't add all addresses")
        return payloads
    }

    private fun buildTransaction(safeInfo: SafeInfo, payloads: List<String>) =
        when (payloads.size) {
            0 -> throw NoNeedToRecoverSafeException(safeInfo.address)
            1 -> SafeTransaction(
                Transaction(safeInfo.address, data = payloads.first()),
                TransactionExecutionRepository.Operation.CALL
            )
            else ->
                SafeTransaction(
                    Transaction(
                        BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!, data = MultiSend.MultiSend.encode(
                            Solidity.Bytes(
                                payloads.joinToString(separator = "") {
                                    SolidityBase.encodeFunctionArguments(
                                        Solidity.UInt8(BigInteger.ZERO),
                                        safeInfo.address,
                                        Solidity.UInt256(BigInteger.ZERO),
                                        Solidity.Bytes(it.hexStringToByteArray())
                                    )
                                }.hexStringToByteArray()
                            )
                        )
                    ), TransactionExecutionRepository.Operation.DELEGATE_CALL
                )
        }

    private fun prepareTransaction(
        safeInfo: SafeInfo,
        transaction: SafeTransaction,
        signingAccounts: SigningAccounts,
        safeOwner: AccountsRepository.SafeOwner
    ) =
        tokenRepository.loadPaymentToken(safeInfo.address)
            .flatMap { executionRepository.loadExecuteInformation(safeInfo.address, it.address, transaction, safeOwner) }
            .flatMap { executionInfo ->
                executionRepository.calculateHash(
                    safeInfo.address,
                    executionInfo.transaction,
                    executionInfo.txGas,
                    executionInfo.dataGas,
                    executionInfo.gasPrice,
                    executionInfo.gasToken,
                    executionInfo.safeVersion
                )
                    .map { it to executionInfo }
            }
            .flatMap { (hash, executionInfo) ->
                Single.zip(
                    accountsRepository.sign(signingAccounts.first, hash),
                    accountsRepository.sign(signingAccounts.second, hash),
                    BiFunction { sig1: Signature, sig2: Signature -> sig1 to sig2 to executionInfo }
                )
            }
            .map<InputRecoveryPhraseContract.ViewUpdate> { (signatures, executionInfo) ->
                InputRecoveryPhraseContract.ViewUpdate.RecoverData(
                    executionInfo,
                    listOf(signatures.first, signatures.second),
                    safeOwner
                )
            }
            .onErrorReturn { InputRecoveryPhraseContract.ViewUpdate.RecoverDataError(errorHandler.translate(it)) }

    data class NoNeedToRecoverSafeException(val safeAddress: Solidity.Address) : IllegalStateException("Safe is already in expected state!")


    companion object {
        private val SENTINEL = "0x01".asEthereumAddress()!!
    }
}
