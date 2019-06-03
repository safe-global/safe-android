package pm.gnosis.heimdall.ui.transactions.create

import android.content.Context
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.transactions.builder.AssetTransferTransactionBuilder
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.removeHexPrefix
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

class CreateAssetTransferViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val executionRepository: TransactionExecutionRepository,
    private val tokenRepository: TokenRepository
) : CreateAssetTransferContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    override fun processInput(
        safe: Solidity.Address,
        tokenAddress: Solidity.Address,
        reviewEvents: Observable<Unit>
    ) = ObservableTransformer<Input, Result<ViewUpdate>> { input ->
        loadTokenInfo(safe, tokenAddress).publish { tokenInfo ->
            // Publish token info if available and parse input with token info for next steps
            Observable.merge(
                tokenInfo.flatMap {
                    it.toNullable()?.let {
                        Observable.just(DataResult(ViewUpdate.TokenInfo(it)))
                    } ?: Observable.empty<Result<ViewUpdate>>()
                },
                handleInput(safe, reviewEvents, input, tokenInfo)
            )
        }
    }

    private fun handleInput(
        safe: Solidity.Address,
        reviewEvents: Observable<Unit>,
        input: Observable<Input>,
        tokenInfo: Observable<Optional<ERC20TokenWithBalance>>
    ) =
        Observable.combineLatest(
            input.flatMap(::parseInput),
            tokenInfo,
            BiFunction { checkedInput: Pair<Solidity.Address?, BigDecimal?>, token: Optional<ERC20TokenWithBalance> ->
                checkedInput to token.toNullable()
            }
        )
            .switchMap<Result<ViewUpdate>> { (input, token) ->
                val updates = mutableListOf<Observable<Result<ViewUpdate>>>()
                val (address, value) = input
                if (address == null || value == null) {
                    updates += Observable.just<Result<ViewUpdate>>(
                        DataResult(
                            ViewUpdate.InvalidInput(
                                value == null,
                                address == null
                            )
                        )
                    )
                } else if (token != null) {
                    updates += setupTokenCheck(safe, token, address, value, reviewEvents)
                }
                Observable.concat(updates)
            }

    private fun parseInput(input: Input): Observable<Pair<Solidity.Address?, BigDecimal?>> =
        Observable.fromCallable {
            val amount = input.amount.toBigDecimalOrNull()
            // Value should not be zero
            input.address to if (amount != BigDecimal.ZERO) amount else null
        }

    private fun loadTokenInfo(safe: Solidity.Address, tokenAddress: Solidity.Address) =
        tokenRepository.loadToken(tokenAddress)
            .emitAndNext(
                emit = { ERC20TokenWithBalance(it, null).toOptional() },
                next = { loadBalance(safe, it).map { it.toOptional() } }
            )
            .startWith(None)

    private fun loadBalance(address: Solidity.Address, token: ERC20Token) =
        tokenRepository.loadTokenBalances(address, listOf(token))
            .map {
                val (erc20Token, balance) = it.first()
                ERC20TokenWithBalance(erc20Token, balance)
            }
            .onErrorReturn { ERC20TokenWithBalance(token, null) }

    private fun setupTokenCheck(
        safe: Solidity.Address,
        token: ERC20TokenWithBalance,
        recipient: Solidity.Address,
        value: BigDecimal,
        reviewEvents: Observable<Unit>
    ): Observable<Result<ViewUpdate>> {
        val amount = value.multiply(BigDecimal(10).pow(token.token.decimals)).toBigInteger()
        // Not enough funds
        if (token.balance == null || token.balance < amount) return Observable.just(
            DataResult(
                ViewUpdate.InvalidInput(
                    true,
                    false
                )
            )
        )
        val data = TransactionData.AssetTransfer(token.token.address, amount, recipient)
        return Observable.merge(
            estimate(safe, data, token.token, token.balance),
            reviewEvents
                .subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    val intent = ReviewTransactionActivity.createIntent(context, safe, data)
                    ViewUpdate.StartReview(intent)
                }
                .mapToResult()
        )
    }

    private fun estimate(safe: Solidity.Address, data: TransactionData.AssetTransfer, transferToken: ERC20Token, assetBalance: BigInteger) =
        tokenRepository.loadPaymentToken().map { it to AssetTransferTransactionBuilder.build(data) }
            .flatMap<ViewUpdate> { (gasToken, transaction) ->
                executionRepository.loadExecuteInformation(safe, gasToken.address, transaction)
                    .map { execInfo: TransactionExecutionRepository.ExecuteInformation ->
                        val estimate = execInfo.gasCosts()
                        val assetBalanceAfterTransfer =
                            if (data.token == gasToken.address) null else ERC20TokenWithBalance(transferToken, assetBalance - data.amount)
                        val gasTokenBalanceAfterTransfer =
                            execInfo.balance - estimate - (if (data.token == gasToken.address) data.amount else BigInteger.ZERO)
                        val canExecute = gasTokenBalanceAfterTransfer >= BigInteger.ZERO &&
                                (assetBalanceAfterTransfer == null || gasTokenBalanceAfterTransfer >= BigInteger.ZERO)
                        ViewUpdate.Estimate(
                            ERC20TokenWithBalance(gasToken, gasTokenBalanceAfterTransfer),
                            estimate,
                            assetBalanceAfterTransfer,
                            canExecute
                        )
                    }
            }
            .toObservable()
            .onErrorReturn {
                Timber.e(it)
                ViewUpdate.EstimateError(errorHandler.translate(it))
            }
            .mapToResult()
}
