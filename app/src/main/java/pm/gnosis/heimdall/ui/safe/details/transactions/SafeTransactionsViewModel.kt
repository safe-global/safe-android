package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import android.content.Intent
import com.gojuno.koptional.None
import com.gojuno.koptional.toOptional
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.impls.GnosisSafeTransactionRepository
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.stringWithNoTrailingZeroes
import java.math.BigInteger
import javax.inject.Inject

class SafeTransactionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository,
    private val safeTransactionsRepository: GnosisSafeTransactionRepository,
    private val tokenRepository: TokenRepository,
    private val transactionDetailsRepository: TransactionDetailsRepository
) : SafeTransactionsContract() {

    private var address: Solidity.Address? = null

    override fun setup(address: Solidity.Address) {
        this.address = address
    }

    override fun observeTransactions(): Flowable<out Result<Adapter.Data<String>>> {
        return this.address?.let {
            safeRepository.observeTransactionDescriptions(it)
                .scanToAdapterData()
                .mapToResult()
        } ?: Flowable.empty<Result<Adapter.Data<String>>>()
    }

    override fun loadTransactionDetails(id: String): Single<Pair<TransactionDetails, TransferInfo?>> =
        address?.let {
            transactionDetailsRepository.loadTransactionDetails(id)
                .flatMap { details ->
                    when (details.type) {
                        TransactionType.ETHER_TRANSFER -> {
                            // We always want to display the complete amount (all 18 decimals)
                            val value = (details.transaction.wrapped.value ?: Wei.ZERO).toEther().stringWithNoTrailingZeroes()
                            val symbol = context.getString(R.string.currency_eth)
                            Single.just(details to TransferInfo(value, symbol))
                        }
                        TransactionType.TOKEN_TRANSFER -> {
                            (details.data as? TokenTransferData)?.let {
                                loadTokenValue(details.transaction.wrapped.address, it.tokens)
                                    .map { details to it.toNullable() }
                            }
                        }
                        else -> null
                    } ?: Single.just(details to null)
                }
        } ?: Single.error(IllegalStateException())

    override fun observeTransactionStatus(id: String): Observable<TransactionExecutionRepository.PublishStatus> =
        safeTransactionsRepository.observePublishStatus(id)

    override fun transactionSelected(id: String): Single<Intent> = Single.error(NotImplementedError()) // TODO: implement

    private fun loadTokenValue(token: Solidity.Address, value: BigInteger) =
        tokenRepository.observeToken(token)
            .map { TransferInfo(it.convertAmount(value).setScale(5).stringWithNoTrailingZeroes(), it.symbol).toOptional() }
            .first(None)
}
