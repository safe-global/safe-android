package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.*
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.layout_create_safe_payment_token.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensActivity
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import java.lang.ref.WeakReference
import java.math.BigInteger
import javax.inject.Inject

abstract class CreateSafePaymentTokenContract : ViewModel() {
    abstract val state: LiveData<State>

    abstract fun setup(authenticatorInfo: AuthenticatorSetupInfo?, additionalOwners: List<Solidity.Address>)

    data class State(val paymentToken: ERC20Token?, val fee: BigInteger?, val canGoNext: Boolean, val viewAction: ViewAction?)

    sealed class ViewAction {
        data class ShowError(val error: Throwable) : ViewAction()
        data class ShowSafe(val safe: Solidity.Address) : ViewAction()
    }

    abstract fun loadPaymentToken()
    abstract fun createSafe()
}

class CreateSafePaymentTokenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: ApplicationModule.AppCoroutineDispatchers,
    private val accountsRepository: AccountsRepository,
    private val safeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository
) : CreateSafePaymentTokenContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    private val coroutineErrorHandler = CoroutineExceptionHandler { _, e ->
        viewModelScope.launch { updateState { copy(viewAction = ViewAction.ShowError(errorHandler.translate(e)), canGoNext = true) } }
    }

    /*
     * Setup logic
     */
    private var authenticatorInfo: AuthenticatorSetupInfo? = null
    private lateinit var additionalOwners: List<Solidity.Address>

    override fun setup(authenticatorInfo: AuthenticatorSetupInfo?, additionalOwners: List<Solidity.Address>) {
        this.authenticatorInfo = authenticatorInfo
        this.additionalOwners = additionalOwners
    }

    /*
     * State management
     */
    private val stateChannel = ConflatedBroadcastChannel(State(null, null, false, null))

    override val state = liveData {
        for (s in stateChannel.openSubscription()) emit(s)
    }

    private fun currentState() = stateChannel.value

    private suspend fun updateState(update: State.() -> State) {
        try {
            val currentState = currentState()
            val nextState = currentState.run(update)
            // Reset view action if the same
            stateChannel.send(if (nextState.viewAction === currentState.viewAction) nextState.copy(viewAction = null) else nextState)
        } catch (e: Exception) {
            // Could not submit update
            Timber.e(e)
        }
    }

    private var loadingJob: WeakReference<Job>? = null
    override fun loadPaymentToken() {
        if (loadingJob?.get()?.isActive == true) return
        loadingJob = weak(viewModelScope.launch(dispatchers.background + coroutineErrorHandler) {
            updateState { copy(canGoNext = false) }
            val paymentToken = tokenRepository.loadPaymentToken().await()
            // Different token we should clean the previous fee
            if (currentState().paymentToken != paymentToken) updateState { copy(paymentToken = paymentToken, fee = null, canGoNext = true) }
            // Same token we can leave the previous fee and update it later
            else updateState { copy(paymentToken = paymentToken, canGoNext = true) }
            val creationFees = tokenRepository.loadPaymentTokensWithCreationFees(additionalOwners.size + 1L).await()
            creationFees.find { it.first.address == paymentToken.address }?.let { (token, fee) ->
                updateState { copy(paymentToken = token, fee = fee, canGoNext = true) }
            }
        })
    }

    private var deploymentJob: WeakReference<Job>? = null
    override fun createSafe() {
        if (deploymentJob?.get()?.isActive == true) return
        deploymentJob = weak(viewModelScope.launch(dispatchers.background + coroutineErrorHandler) {
            updateState { copy(canGoNext = false) }
            val owner = authenticatorInfo?.safeOwner ?: accountsRepository.createOwner().await()
            val owners = listOf(owner.address) + additionalOwners
            val deploymentData = safeRepository.triggerSafeDeployment(owners, owners.size - 2).await()
            authenticatorInfo?.let { safeRepository.saveAuthenticatorInfo(it.authenticator) }
            safeRepository.addPendingSafe(deploymentData.safe, null, deploymentData.payment, deploymentData.paymentToken).await()
            safeRepository.saveOwner(deploymentData.safe, owner).await()
            updateState { copy(viewAction = ViewAction.ShowSafe(deploymentData.safe)) }
        })
    }
}

class CreateSafePaymentTokenActivity : ViewModelActivity<CreateSafePaymentTokenContract>() {
    @Inject
    lateinit var picasso: Picasso

    override fun layout() = R.layout.layout_create_safe_payment_token

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId(): ScreenId? = ScreenId.CREATE_SAFE_PAYMENT_TOKEN


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val additionalOwners = nullOnThrow { intent.getStringArrayListExtra(EXTRA_ADDITIONAL_OWNERS)?.map { it.asEthereumAddress()!! } }
            ?: run {
                finish()
                return
            }
        viewModel.setup(intent.getAuthenticatorInfo(), additionalOwners)
        viewModel.state.observe(this, Observer { state ->
            create_safe_payment_token_next_btn.isEnabled = state.canGoNext
            create_safe_payment_token_change_token_btn.isEnabled = state.canGoNext
            create_safe_payment_token_fee_lbl.text = state.fee?.let { state.paymentToken?.displayString(it, false) }
            val paymentTokenSymbol = state.paymentToken?.symbol
            create_safe_payment_token_symbol_lbl.text = paymentTokenSymbol
            create_safe_payment_token_next_btn.text =
                paymentTokenSymbol?.let { getString(R.string.pay_with, it) } ?: getString(R.string.continue_text)
            create_safe_payment_token_icon_img.loadTokenImage(picasso, state.paymentToken)

            state.viewAction?.let { performAction(it) }
        })

        create_safe_payment_token_change_token_btn.setOnClickListener {
            startActivity(PaymentTokensActivity.createIntent(this, ownerCount = additionalOwners.size + 1L))
        }
        create_safe_payment_token_next_btn.setOnClickListener { viewModel.createSafe() }

    }

    override fun onStart() {
        super.onStart()
        viewModel.loadPaymentToken()
    }

    private fun performAction(viewAction: CreateSafePaymentTokenContract.ViewAction): Any =
        when (viewAction) {
            is CreateSafePaymentTokenContract.ViewAction.ShowError ->
                errorSnackbar(create_safe_payment_token_content_scroll, viewAction.error)
            is CreateSafePaymentTokenContract.ViewAction.ShowSafe ->
                startActivity(SafeMainActivity.createIntent(this, viewAction.safe))
        }

    companion object {
        private const val EXTRA_ADDITIONAL_OWNERS = "extra.list_string.additional_owners"

        fun createIntent(
            context: Context,
            authenticatorInfo: AuthenticatorSetupInfo?,
            additionalOwners: List<Solidity.Address>
        ) =
            Intent(context, CreateSafePaymentTokenActivity::class.java).apply {
                authenticatorInfo.put(this)
                putStringArrayListExtra(EXTRA_ADDITIONAL_OWNERS, additionalOwners.mapTo(ArrayList()) { it.asEthereumAddressString() })
            }
    }
}