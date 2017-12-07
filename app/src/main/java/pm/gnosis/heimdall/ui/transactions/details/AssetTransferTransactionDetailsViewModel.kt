package pm.gnosis.heimdall.ui.transactions.details

import android.content.Context
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TokenTransferData
import pm.gnosis.heimdall.data.repositories.TransactionDetailsRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import pm.gnosis.utils.nullOnThrow
import java.math.BigDecimal
import java.math.BigInteger
import javax.inject.Inject


class AssetTransferTransactionDetailsViewModel @Inject constructor(
        private var detailsRepository: TransactionDetailsRepository,
        private var tokenRepository: TokenRepository
) : AssetTransferTransactionDetailsContract() {

    override fun loadFormData(transaction: Transaction?): Single<FormData> =
            transaction?.let {
                detailsRepository.loadTransactionDetails(transaction)
                        .flatMap { details ->
                            when (details.data) {
                                is TokenTransferData -> {
                                    tokenRepository.loadToken(details.transaction.address)
                                            .map {
                                                FormData(it.address, details.data.recipient, details.data.tokens, it)
                                            }
                                            .onErrorReturn {
                                                FormData(details.transaction.address, details.data.recipient, details.data.tokens)
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

    override fun inputTransformer(context: Context, originalTransaction: Transaction?): ObservableTransformer<CombinedRawInput, Result<Transaction>> =
            ObservableTransformer {
                it.scan { old, new -> old.diff(new) }
                        .map {
                            val to = it.to.first.hexAsEthereumAddressOrNull()
                            val amount = nullOnThrow { BigDecimal(it.amount.first) }
                            val token = it.token.first
                            var errorFields = 0
                            var showToast = false
                            if (to == null) {
                                errorFields = errorFields or AssetTransferTransactionDetailsFragment.TransactionInputException.TO_FIELD
                                showToast = showToast or it.to.second
                            }
                            if (amount == null) {
                                errorFields = errorFields or AssetTransferTransactionDetailsFragment.TransactionInputException.AMOUNT_FIELD
                                showToast = showToast or it.amount.second
                            }
                            if (token == null) {
                                errorFields = errorFields or AssetTransferTransactionDetailsFragment.TransactionInputException.TOKEN_FIELD
                                showToast = showToast or it.token.second
                            }
                            if (errorFields > 0) {
                                ErrorResult<Transaction>(AssetTransferTransactionDetailsFragment.TransactionInputException(context, errorFields, showToast))
                            } else {
                                val nonce = originalTransaction?.nonce ?: BigInteger.valueOf(System.currentTimeMillis())
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

}