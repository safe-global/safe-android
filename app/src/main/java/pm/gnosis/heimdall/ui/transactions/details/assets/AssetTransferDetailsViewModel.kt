package pm.gnosis.heimdall.ui.transactions.details.assets

import android.content.Context
import com.gojuno.koptional.Optional
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.ui.transactions.exceptions.TransactionInputException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.hexAsEthereumAddressOrNull
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
                detailsRepository.loadTransactionDetails(transaction)
                        .flatMap { details ->
                            when (details.data) {
                                is TokenTransferData -> {
                                    // If recipient is 0x0 we set the value to null to force user input
                                    val recipient = if (clearDefaults && details.data.recipient == BigInteger.ZERO) null else details.data.recipient
                                    // If the token count is 0x0 we set it to null to force user input
                                    val tokens = if (clearDefaults && details.data.tokens == BigInteger.ZERO) null else details.data.tokens
                                    tokenRepository.loadToken(details.transaction.address)
                                            .map {
                                                FormData(it.address, recipient, tokens, it)
                                            }
                                            .onErrorReturn {
                                                FormData(details.transaction.address, recipient, tokens)
                                            }
                                }
                                else ->
                                    Single.just(FormData(ETHER_TOKEN.address, details.transaction.address, details.transaction.value?.value ?: BigInteger.ZERO, ETHER_TOKEN))
                            }
                        }
                        .onErrorReturnItem(FormData())
            } ?: Single.just(FormData())


    override fun observeTokens(defaultToken: BigInteger?, safeAddress: BigInteger?): Observable<State> =
            tokenRepository.loadTokens()
                    .onErrorReturnItem(emptyList())
                    .map { arrayListOf(ETHER_TOKEN) + it }
                    .flatMapObservable {
                        val tokensNoBalance = it.map { ERC20TokenWithBalance(it, null) }
                        if (safeAddress != null)
                            tokenRepository.loadTokenBalances(safeAddress, it)
                                    .map {
                                        it.mapNotNull { (token, balance) ->
                                            if (token.verified && (balance == BigInteger.ZERO)) null
                                            else ERC20TokenWithBalance(token, balance)
                                        }
                                    }
                                    .onErrorReturnItem(tokensNoBalance)
                        else
                            Observable.just(tokensNoBalance)
                    }
                    .map { State(getCurrentSelectedTokenIndex(defaultToken, it), it) }


    private fun getCurrentSelectedTokenIndex(selectedToken: BigInteger?, tokens: List<ERC20TokenWithBalance>): Int {
        selectedToken?.let {
            tokens.forEachIndexed { index, token -> if (token.token.address == selectedToken) return index }
        }
        return 0
    }

    override fun inputTransformer(originalTransaction: Transaction?): ObservableTransformer<InputEvent, Result<Transaction>> =
            ObservableTransformer {
                it.scan { old, new -> old.diff(new) }
                        .map {
                            val to = it.to.first.hexAsEthereumAddressOrNull()
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
                                ErrorResult<Transaction>(TransactionInputException(context, errorFields, showToast))
                            } else {
                                val nonce = originalTransaction?.nonce
                                val tokenAmount = amount!!.multiply(BigDecimal(10).pow(token!!.token.decimals)).toBigInteger()
                                val transaction = when (token.token) {
                                    ETHER_TOKEN -> {
                                        val value = Wei(tokenAmount)
                                        Transaction(to!!, value = value, nonce = nonce)
                                    }
                                    else -> {
                                        val transferTo = Solidity.Address(to!!)
                                        val transferAmount = Solidity.UInt256(tokenAmount)
                                        val data = StandardToken.Transfer.encode(transferTo, transferAmount)
                                        Transaction(token.token.address, data = data, nonce = nonce)
                                    }
                                }
                                DataResult(transaction)
                            }
                        }
            }

    override fun transactionTransformer(): ObservableTransformer<Optional<Transaction>, Result<Transaction>> =
            ObservableTransformer {
                it.flatMapSingle {
                    val transaction = it.toNullable()
                    transaction?.let {
                        detailsRepository.loadTransactionDetails(it)
                                .map(::checkDetails)
                                .mapToResult()
                    } ?: Single.just(ErrorResult(IllegalStateException()))
                }
            }

    private fun checkDetails(details: TransactionDetails): Transaction {
        return when (details.type) {
            TransactionType.TOKEN_TRANSFER -> {
                val data = details.data as? TokenTransferData
                if (data == null || data.tokens == BigInteger.ZERO) {
                    throw TransactionInputException(context, TransactionInputException.AMOUNT_FIELD, true)
                }
                details.transaction
            }
            TransactionType.ETHER_TRANSFER -> {
                if (details.transaction.value == null || details.transaction.value?.value == BigInteger.ZERO) {
                    throw TransactionInputException(context, TransactionInputException.AMOUNT_FIELD, true)
                }
                details.transaction
            }
            else ->
                throw IllegalStateException()
        }
    }

}