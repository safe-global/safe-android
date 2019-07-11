package pm.gnosis.heimdall.ui.tokens.payment

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.splash.ViewAction
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import timber.log.Timber
import java.lang.Exception
import java.math.BigInteger
import javax.inject.Inject

abstract class PaymentTokensContract : ViewModel() {
    abstract val state: LiveData<State>
    abstract val paymentToken: LiveData<ERC20Token>

    abstract fun setup(safe: Solidity.Address?, metricType: MetricType)
    abstract fun setPaymentToken(token: ERC20Token)
    abstract fun loadPaymentTokens()

    data class State(val items: List<PaymentToken>, val loading: Boolean, val viewAction: ViewAction?)

    data class PaymentToken(val erc20Token: ERC20Token, val metricValue: String?, val selectable: Boolean)

    sealed class ViewAction {
        data class ShowError(val error: Throwable) : ViewAction()
    }

    sealed class MetricType {
        object Balance : MetricType()
        data class TransactionFees(val transaction: SafeTransaction) : MetricType()
        data class CreationFees(val numbersOwners: Long) : MetricType()
    }
}


@ExperimentalCoroutinesApi
class PaymentTokensViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: ApplicationModule.AppCoroutineDispatchers,
    private val tokenRepository: TokenRepository
) : PaymentTokensContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    /*
     * Setup logic
     */
    private var safe: Solidity.Address? = null
    private lateinit var metricType: MetricType
    override fun setup(safe: Solidity.Address?, metricType: MetricType) {
        this.safe = safe
        this.metricType = metricType
    }

    /*
     * State management
     */
    private val stateChannel = ConflatedBroadcastChannel(State(emptyList(), false, null))

    override val state = liveData {
        loadPaymentTokens()
        for (state in stateChannel.openSubscription()) emit(state)
    }

    private suspend fun updateState(items: List<PaymentToken>? = null, viewAction: ViewAction? = null, loading: Boolean? = null) {
        try {
            val currentState = stateChannel.value
            val newSate = currentState.copy(
                items = items ?: currentState.items,
                viewAction = if (viewAction == currentState.viewAction) null else viewAction,
                loading = loading ?: currentState.loading
            )
            stateChannel.send(newSate)
        } catch (e: Exception) {
            // Could not submit update
            Timber.e(e)
        }
    }

    /*
     * State processing
     */
    override fun loadPaymentTokens() {
        viewModelScope.launch(dispatchers.background) {
            if (stateChannel.value.loading) return@launch
            try {
                updateState(loading = true)
                metricType.let {
                    when (it) {
                        MetricType.Balance -> loadPaymentTokensWithBalances()
                        is MetricType.CreationFees -> loadPaymentTokensWithCreationFees(it.numbersOwners)
                        is MetricType.TransactionFees -> loadPaymentTokensWithTransactionFees(it.transaction)
                    }
                }
            } catch (e: Exception) {
                updateState(viewAction = ViewAction.ShowError(errorHandler.translate(e)), loading = false)
            }
        }
    }

    private suspend fun loadPaymentTokensWithBalances() {
        val tokens = tokenRepository.loadPaymentTokens().await()
        finishLoadingItemsWithMethod(
            tokenRepository.loadTokenBalances(safe!!, tokens).awaitFirst()
        ) { balance -> balance == null || balance > BigInteger.ZERO }
    }

    private suspend fun loadPaymentTokensWithCreationFees(numbersOwners: Long) {
        finishLoadingItemsWithMethod(tokenRepository.loadPaymentTokensWithCreationFees(numbersOwners).await())
    }

    private suspend fun loadPaymentTokensWithTransactionFees(transaction: SafeTransaction) {
        finishLoadingItemsWithMethod(tokenRepository.loadPaymentTokensWithTransactionFees(safe!!, transaction).await())
    }

    private suspend fun finishLoadingItemsWithMethod(
        itemsWithMetric: List<Pair<ERC20Token, BigInteger?>>,
        selectableCheck: ((BigInteger?) -> Boolean)? = null
    ) {
        val items = itemsWithMetric.map { (token, balance) ->
            val metricValue = balance?.let { token.displayString(balance, false) }
            PaymentToken(token, metricValue, selectableCheck?.invoke(balance) ?: true)
        }
        updateState(items = items, loading = false)
    }

    /*
     * Payment token management
     */
    private val paymentTokenChannel = BroadcastChannel<ERC20Token>(Channel.CONFLATED)

    override val paymentToken = liveData {
        // Set initial value on channel
        loadPaymentToken()
        for (i in paymentTokenChannel.openSubscription()) emit(i)
    }

    private suspend fun loadPaymentToken() {
        viewModelScope.launch(dispatchers.background) {
            paymentTokenChannel.send(tokenRepository.loadPaymentToken(safe).await())
        }
    }

    override fun setPaymentToken(token: ERC20Token) {
        viewModelScope.launch(dispatchers.background) {
            tokenRepository.setPaymentToken(safe, token).await()
            paymentTokenChannel.send(token)
        }
    }

    /*
     * Clean up
     */
    override fun onCleared() {
        super.onCleared()
        stateChannel.cancel()
        paymentTokenChannel.cancel()
    }

}
