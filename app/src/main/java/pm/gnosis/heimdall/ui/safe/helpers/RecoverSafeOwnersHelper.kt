package pm.gnosis.heimdall.ui.safe.helpers

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.heimdall.ui.transactions.builder.MultiSendTransactionBuilder
import pm.gnosis.mnemonic.Bip39ValidationResult
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.nullOnThrow
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

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
        authenticatorAddress: Solidity.Address?,
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
        authenticatorAddress: Solidity.Address?,
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
                            .concatWith(processRecoveryPhrase(input, it.data, authenticatorAddress, safeOwner))
                }
            }

    private fun processRecoveryPhrase(
        input: InputRecoveryPhraseContract.Input,
        safeInfo: SafeInfo,
        authenticatorAddress: Solidity.Address?,
        safeOwner: AccountsRepository.SafeOwner?
    ): Observable<InputRecoveryPhraseContract.ViewUpdate> =
        input.phrase
            .flatMapSingle {
                accountsRepository.createOwnersFromPhrase(it.toString(), listOf(0, 1))
                    .map { accounts ->
                        val account0 = accounts[0]
                        val account1 = accounts[1]

                        // Check that the accounts generated by the mnemonic are owners
                        require(safeInfo.owners.contains(account0.address) && safeInfo.owners.contains(account1.address))

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
                                addressesToSwapIn = listOfNotNull(safeOwner.address, authenticatorAddress).toSet()
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
        val newOwners = addressesToAdd.union(addressesToKeep)
        val targetThreshold = max(1L, newOwners.size - 2L)
        val payloads = mutableListOf<String>()
        val remainingNewAddresses = addressesToAdd.toMutableList()
        for (i in safeInfo.owners.size - 1 downTo 0) {
            val safeOwner = safeInfo.owners[i]
            if (remainingNewAddresses.remove(safeOwner)) continue
            if (newOwners.contains(safeOwner)) continue

            var newOwner: Solidity.Address? = null
            while (remainingNewAddresses.isNotEmpty()) {
                newOwner = remainingNewAddresses.removeAt(remainingNewAddresses.lastIndex)
                if (!safeInfo.owners.contains(newOwner)) break
                newOwner = null
            }
            val pointerAddress = nullOnThrow { safeInfo.owners[i - 1] } ?: SENTINEL
            newOwner?.let {
                // Swap owner
                payloads += GnosisSafe.SwapOwner.encode(
                    prevOwner = pointerAddress,
                    oldOwner = safeOwner,
                    newOwner = newOwner
                )
            } ?: run {
                // Remove unwanted owner
                payloads += GnosisSafe.RemoveOwner.encode(
                    prevOwner = pointerAddress,
                    owner = safeOwner,
                    _threshold = Solidity.UInt256(BigInteger.valueOf(targetThreshold))
                )
            }
        }
        // Add missing owners
        while(remainingNewAddresses.isNotEmpty()) {
            val newOwner = remainingNewAddresses.removeAt(remainingNewAddresses.lastIndex)
            val threshold = if (remainingNewAddresses.isEmpty()) targetThreshold else 1
            payloads += GnosisSafe.AddOwnerWithThreshold.encode(owner = newOwner, _threshold = Solidity.UInt256(threshold.toBigInteger()))
        }
        check(remainingNewAddresses.isEmpty()) { "Couldn't add all addresses" }
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
                MultiSendTransactionBuilder.build(
                    payloads.map { SafeTransaction(Transaction(safeInfo.address, data=it), TransactionExecutionRepository.Operation.CALL) }
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
