package pm.gnosis.heimdall.ui.transactions.details.assets

import android.content.Context
import com.gojuno.koptional.Optional
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.nullOnThrow
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject

class AssetTransferDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private var detailsRepository: TransactionDetailsRepository,
    private var tokenRepository: TokenRepository
) : AssetTransferDetailsContract() {

    override fun loadFormData(transaction: Transaction?, clearDefaults: Boolean): Single<FormData> =
        transaction?.let {
            detailsRepository.loadTransactionData(transaction)
                .flatMap {
                    val data = it.toNullable()
                    when (data) {
                        is TokenTransferData -> {
                            // If recipient is 0x0 we set the value to null to force user input
                            val recipient = if (clearDefaults && data.recipient == Solidity.Address(BigInteger.ZERO)) null else data.recipient
                            // If the token count is 0x0 we set it to null to force user input
                            val tokens = if (clearDefaults && data.tokens == BigInteger.ZERO) null else data.tokens
                            tokenRepository.loadToken(transaction.address)
                                .map { FormData(recipient, tokens, it) }
                                .onErrorReturn { FormData(recipient, tokens) }
                        }
                        else -> {
                            // If recipient is 0x0 we set the value to null to force user input
                            val recipient =
                                if (clearDefaults && transaction.address == Solidity.Address(BigInteger.ZERO)) null else transaction.address
                            Single.just(FormData(recipient, transaction.value?.value, ETHER_TOKEN))
                        }
                    }
                }
                .onErrorReturnItem(FormData())
        } ?: Single.just(FormData())

    override fun loadTokenInfo(safeAddress: Solidity.Address, token: ERC20Token): Observable<Result<ERC20TokenWithBalance>> =
        tokenRepository.loadTokenBalances(safeAddress, listOf(token))
            .map {
                val info = it.first()
                ERC20TokenWithBalance(info.first, info.second)
            }
            .mapToResult()

    override fun inputTransformer(originalTransaction: SafeTransaction?): ObservableTransformer<InputEvent, Result<SafeTransaction>> =
        ObservableTransformer {
            it.scan { old, new -> old.diff(new) }
                .map {
                    val to = it.to.first.asEthereumAddress()
                    val amount = nullOnThrow { BigDecimal(it.amount.first) }
                    val token = it.token.first
                    var errorFields = 0
                    var showToast = false
                    if (to == null) {
                        errorFields = errorFields or TransactionInputException.TO_FIELD
                        showToast = showToast or it.to.second
                    }
                    if (amount == null || amount == BigDecimal.ZERO) {
                        errorFields = errorFields or TransactionInputException.AMOUNT_FIELD
                        showToast = showToast or it.amount.second
                    }
                    if (token == null) {
                        errorFields = errorFields or TransactionInputException.TOKEN_FIELD
                        showToast = showToast or it.token.second
                    }
                    if (errorFields > 0) {
                        ErrorResult<SafeTransaction>(TransactionInputException(context, errorFields, showToast))
                    } else {
                        val nonce = originalTransaction?.wrapped?.nonce
                        val tokenAmount = amount!!.multiply(BigDecimal(10).pow(token!!.decimals)).toBigInteger()
                        val transaction = when (token) {
                            ETHER_TOKEN -> {
                                val value = Wei(tokenAmount)
                                Transaction(to!!, value = value, nonce = nonce)
                            }
                            else -> {
                                val transferTo = to!!
                                val transferAmount = Solidity.UInt256(tokenAmount)
                                val data = StandardToken.Transfer.encode(transferTo, transferAmount)
                                Transaction(token.address, data = data, nonce = nonce)
                            }
                        }
                        DataResult(SafeTransaction(transaction, TransactionRepository.Operation.CALL))
                    }
                }
        }

    override fun transactionTransformer(): ObservableTransformer<Optional<SafeTransaction>, Result<SafeTransaction>> =
        ObservableTransformer {
            it.flatMapSingle {
                val transaction = it.toNullable()
                transaction?.let {
                    detailsRepository.loadTransactionData(transaction.wrapped)
                        .map { transaction to it.toNullable() }
                        .map(::checkDetails)
                        .mapToResult()
                } ?: Single.just(ErrorResult(IllegalStateException()))
            }
        }

    private fun checkDetails(data: Pair<SafeTransaction, TransactionTypeData?>): SafeTransaction {
        val (transaction, typeData) = data
        return when (typeData) {
            is TokenTransferData -> {
                if (typeData.tokens == BigInteger.ZERO) {
                    throw TransactionInputException(context, TransactionInputException.AMOUNT_FIELD, true)
                }
                transaction
            }
            else -> {
                if (!transaction.wrapped.data.isNullOrBlank()) {
                    throw TransactionInputException(context, TransactionInputException.AMOUNT_FIELD, true)
                }
                if (transaction.wrapped.value == null || transaction.wrapped.value?.value == BigInteger.ZERO) {
                    throw TransactionInputException(context, TransactionInputException.AMOUNT_FIELD, true)
                }
                transaction
            }
        }
    }
}
