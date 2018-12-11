package pm.gnosis.heimdall.ui.safe.recover.extension

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReplaceExtensionSubmitViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val pushServiceRepository: PushServiceRepository,
    private val tokenRepository: TokenRepository,
    private val transactionExecutionRepository: TransactionExecutionRepository
) : ReplaceExtensionSubmitContract() {
    private lateinit var safeTransaction: SafeTransaction
    private lateinit var signature1: Signature
    private lateinit var signature2: Signature
    private lateinit var txGas: BigInteger
    private lateinit var dataGas: BigInteger
    private lateinit var operationalGas: BigInteger
    private lateinit var gasPrice: BigInteger
    private lateinit var gasToken: Solidity.Address
    private lateinit var newChromeExtension: Solidity.Address
    private lateinit var txHash: ByteArray

    override fun setup(
        safeTransaction: SafeTransaction,
        signature1: Signature,
        signature2: Signature,
        txGas: BigInteger,
        dataGas: BigInteger,
        operationalGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        newChromeExtension: Solidity.Address,
        txHash: ByteArray
    ) {
        this.safeTransaction = safeTransaction
        this.signature1 = signature1
        this.signature2 = signature2
        this.txGas = txGas
        this.dataGas = dataGas
        this.operationalGas = operationalGas
        this.gasPrice = gasPrice
        this.gasToken = gasToken
        this.newChromeExtension = newChromeExtension
        this.txHash = txHash
    }

    override fun loadFeeInfo(): Single<ERC20TokenWithBalance> =
        tokenRepository.loadToken(gasToken).map {
            ERC20TokenWithBalance(it, requiredFunds())
        }

    override fun observeSubmitStatus(): Observable<Result<SubmitStatus>> =
        tokenRepository.loadToken(gasToken)
            .flatMapObservable { token ->
                Observable.interval(0, SAFE_BALANCE_REQUEST_INTERVAL, SAFE_BALANCE_REQUEST_TIME_UNIT)
                    .concatMap { _ ->
                        tokenRepository.loadTokenBalances(safeTransaction.wrapped.address, listOf(token))
                            .map { tokenBalances ->
                                if (tokenBalances.size != 1) throw NoTokenBalanceException()
                                tokenBalances.first().let { (token, balance) ->
                                    val canSubmit = requiredFunds() <= balance
                                    SubmitStatus(ERC20TokenWithBalance(token, balance), canSubmit)
                                }
                            }
                            .mapToResult()
                    }
            }

    private fun requiredFunds() = (txGas + dataGas + operationalGas) * gasPrice

    override fun getSafeTransaction() = safeTransaction

    override fun loadSafe() = gnosisSafeRepository.loadSafe(safeTransaction.wrapped.address)

    override fun submitTransaction() =
        transactionExecutionRepository.calculateHash(
            safeAddress = safeTransaction.wrapped.address,
            transaction = safeTransaction,
            txGas = txGas,
            dataGas = dataGas,
            gasPrice = gasPrice,
            gasToken = gasToken
        )
            // Verify if transaction hash matches
            .map { computedTxHash ->
                if (!computedTxHash.contentEquals(txHash)) throw IllegalStateException("Invalid transaction hash")
                else Unit
            }
            // Recover addresses from the signatures
            .flatMap { _ ->
                Single.zip(listOf(signature1, signature2).map { signature ->
                    accountsRepository.recover(txHash, signature).map { it to signature }
                }) { pairs -> (pairs.map { it as Pair<Solidity.Address, Signature> }).toList() }
            }
            // Submit transaction
            .flatMap { signaturesWithAddresses ->
                transactionExecutionRepository.submit(
                    safeAddress = safeTransaction.wrapped.address,
                    transaction = safeTransaction,
                    signatures = signaturesWithAddresses.toMap(),
                    senderIsOwner = false,
                    txGas = txGas,
                    dataGas = dataGas,
                    gasPrice = gasPrice,
                    gasToken = gasToken,
                    addToHistory = true
                )
            }
            .flatMapCompletable {
                pushServiceRepository.propagateSafeCreation(safeTransaction.wrapped.address, setOf(newChromeExtension)).onErrorComplete()
            }
            .mapToResult()

    companion object {
        private const val SAFE_BALANCE_REQUEST_INTERVAL = 5L
        private val SAFE_BALANCE_REQUEST_TIME_UNIT = TimeUnit.SECONDS
    }
}
