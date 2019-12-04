package pm.gnosis.heimdall.ui.safe.recover.safe.submit

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.pending.SafeCreationFundViewModel
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RecoveringSafeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoHelper: CryptoHelper,
    private val executionRepository: TransactionExecutionRepository,
    private val safeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository,
    private var qrCodeGenerator: QrCodeGenerator
) : RecoveringSafeContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun checkSafeState(address: Solidity.Address): Single<Pair<RecoveringSafe, RecoveryState>> =
        safeRepository.loadRecoveringSafe(address)
            .flatMap { safe ->
                if (safe.transactionHash != null)
                    Single.just(safe to RecoveryState.PENDING)
                else
                    requestExecuteInfo(safe).map {
                        when {
                            safe.nonce != it.transaction.wrapped.nonce ||
                                    it.transaction.wrapped.value ?: Wei.ZERO != Wei.ZERO -> safe to RecoveryState.ERROR
                            it.balance < it.gasCosts() -> safe to RecoveryState.CREATED
                            else -> safe to RecoveryState.FUNDED
                        }
                    }
            }
            .onErrorResumeNext { errorHandler.single(it) }

    override fun checkRecoveryStatus(address: Solidity.Address): Single<Solidity.Address> =
        safeRepository.loadRecoveringSafe(address)
            .flatMap { safe ->
                safe.transactionHash?.let {
                    executionRepository.observeTransactionStatus(it)
                        .firstOrError()
                        .flatMap { (success) ->
                            if (success)
                                safeRepository.recoveringSafeToDeployedSafe(safe).andThen(Single.just(safe.address))
                            else throw TransactionExecutionException("transaction execution failed")
                        }
                } ?: throw IllegalStateException()
            }
            .onErrorResumeNext { errorHandler.single(it) }


    override fun observeRecoveryInfo(address: Solidity.Address): Observable<Result<RecoveryInfo>> =
        safeRepository.loadRecoveringSafe(address)
            .toObservable()
            .flatMap {
                safe ->
                qrCodeGenerator.generateQrCode(address.asEthereumAddressChecksumString())
                    .toObservable()
                    .map {
                        safe to it
                    }
            }
            .emitAndNext(
                emit = { (recoveringSafe, qrCode) ->
                    DataResult(
                        RecoveryInfo(
                            recoveringSafe.address.asEthereumAddressChecksumString(),
                            null,
                            recoveringSafe.requiredFunds(),
                            qrCode
                        )
                    )
                },
                next = { (recoveringSafe, qrCode) -> observeTokenRecoveryInfo(recoveringSafe.address.asEthereumAddressChecksumString(), recoveringSafe.requiredFunds(), recoveringSafe.gasToken, qrCode) }
            )

    private fun observeTokenRecoveryInfo(safeAddress: String, requiredFunds: BigInteger, gasTokenAddress: Solidity.Address, qrCode: Bitmap) =
        tokenRepository.loadToken(gasTokenAddress)
            .onErrorResumeNext { error: Throwable -> errorHandler.single(error) }
            .emitAndNext(
                emit = {
                    RecoveryInfo(safeAddress, ERC20TokenWithBalance(it, null), requiredFunds, qrCode)
                },
                next = { token ->
                    tokenRepository.loadTokenBalances(safeAddress.asEthereumAddress()!!, listOf(token))
                        .repeatWhen { it.delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS) }
                        .retryWhen { it.delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS) }
                        .map {
                            it.first()
                        }
                        .map { (token, balance) ->
                            RecoveryInfo(safeAddress, ERC20TokenWithBalance(token, balance), requiredFunds, qrCode)
                        }
                }
            )
            .mapToResult()

    override fun loadRecoveryExecuteInfo(address: Solidity.Address): Single<RecoveryExecuteInfo> =
        safeRepository.loadRecoveringSafe(address)
            .flatMap { safe ->
                executionRepository.loadSafeExecuteState(address, safe.gasToken)
                    .zipWith(tokenRepository.loadToken(safe.gasToken),
                        BiFunction { execState: TransactionExecutionRepository.SafeExecuteState, token: ERC20Token ->
                            val paymentAmount = safe.requiredFunds()
                            val balanceAfterTx = execState.balance - paymentAmount
                            RecoveryExecuteInfo(
                                balanceAfterTx,
                                paymentAmount,
                                token,
                                balanceAfterTx >= BigInteger.ZERO
                            )
                        }
                    )
            }
            .onErrorResumeNext { errorHandler.single(it) }

    override fun submitRecovery(address: Solidity.Address): Single<Solidity.Address> =
        safeRepository.loadRecoveringSafe(address)
            .flatMap { safe ->
                safeRepository.loadInfo(address)
                    .firstOrError()
                    .flatMap { info ->
                        executionRepository.calculateHash(
                            address,
                            buildSafeTransaction(safe),
                            safe.txGas,
                            safe.dataGas,
                            safe.gasPrice,
                            safe.gasToken,
                            info.version
                        )
                            .flatMap { txHash ->
                                Single.zip(
                                    safe.signatures.map { signature ->
                                        Single.fromCallable { cryptoHelper.recover(txHash, signature) to signature }
                                    }
                                ) {
                                    it.associate {
                                        @Suppress("UNCHECKED_CAST") // Unchecked cast is necessary because of rx zip implementation
                                        it as Pair<Solidity.Address, Signature>
                                    }
                                }
                            }
                            .map { safe to it }
                            .flatMap { (safe, signatures) ->
                                executionRepository.submit(
                                    address, buildSafeTransaction(safe), signatures, false,
                                    safe.txGas, safe.dataGas, safe.gasPrice, safe.gasToken, info.version, false
                                )
                                    .flatMap {
                                        safeRepository.updateRecoveringSafe(safe.copy(transactionHash = it.hexAsBigInteger()))
                                            .andThen(Single.just(safe.address))
                                    }
                            }
                    }
            }
            .onErrorResumeNext { errorHandler.single(it) }


    override fun checkRecoveryFunded(address: Solidity.Address): Single<Solidity.Address> =
        safeRepository.loadRecoveringSafe(address)
            .flatMap { safe ->
                val gasCosts = safe.requiredFunds()
                // Create a fake token since only the address is necessary to load the balance
                requestBalance(
                    safe.address,
                    ERC20Token(safe.gasToken, decimals = 0, name = "", symbol = "")
                )
                    .map { if (it < gasCosts) throw SafeCreationFundViewModel.NotEnoughFundsException() }
                    .retryWhen { errors ->
                        errors.delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS)
                    }
                    .map { safe.address }
            }
            .onErrorResumeNext { errorHandler.single(it) }

    private fun requestBalance(address: Solidity.Address, paymentToken: ERC20Token) =
        tokenRepository.loadTokenBalances(address, listOf(paymentToken)).map { it.first().second ?: BigInteger.ZERO }.firstOrError()

    private fun requestExecuteInfo(safe: RecoveringSafe) =
        executionRepository.loadExecuteInformation(
            safe.address,
            safe.gasToken,
            buildSafeTransaction(safe)
        )

    private fun buildSafeTransaction(safe: RecoveringSafe) =
        SafeTransaction(
            Transaction(safe.recoverer, data = safe.data, nonce = safe.nonce),
            safe.operation
        )

    private fun RecoveringSafe.requiredFunds() =
        (txGas + dataGas + operationalGas) * gasPrice

    companion object {
        private const val BALANCE_REQUEST_INTERVAL_SECONDS = 10L
    }
}
