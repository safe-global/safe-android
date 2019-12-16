package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.android.synthetic.main.layout_recovery_phrase_intro.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitFirst
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.handleViewAction
import pm.gnosis.heimdall.ui.recoveryphrase.RecoveryPhraseIntroActivity
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import javax.inject.Inject

abstract class SetupNewRecoveryPhraseIntroContract(context: Context, appDispatcher: ApplicationModule.AppCoroutineDispatchers) :
    BaseStateViewModel<SetupNewRecoveryPhraseIntroContract.State>(context, appDispatcher) {

    abstract fun setup(safe: Solidity.Address)

    abstract fun performNext()

    data class State(
        val loading: Boolean,
        override var viewAction: ViewAction?
    ) : BaseStateViewModel.State
}

class SetupNewRecoveryPhraseIntroViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    appDispatcher: ApplicationModule.AppCoroutineDispatchers,
    private val safeRepository: GnosisSafeRepository
) : SetupNewRecoveryPhraseIntroContract(context, appDispatcher) {

    override val state = liveData {
        for (event in stateChannel.openSubscription()) emit(event)
    }

    private val loadingErrorHandler = CoroutineExceptionHandler { context, e ->
        viewModelScope.launch { updateState { copy(loading = false) } }
        coroutineErrorHandler.handleException(context, e)
    }

    private fun loadingLaunch(block: suspend CoroutineScope.() -> Unit) = safeLaunch(loadingErrorHandler, block)

    private lateinit var safe: Solidity.Address

    override fun setup(safe: Solidity.Address) {
        this.safe = safe
    }

    override fun performNext() {
        loadingLaunch {
            updateState { copy(loading = true) }
            val info = safeRepository.loadInfo(safe).awaitFirst()
            check(info.isOwner) { "This device is not an owner of the selected Safe!" }
            if (info.requiredConfirmations == 1L) {
                val viewAction = ViewAction.StartActivity(SetupNewRecoveryPhraseActivity.createIntent(context, safe, null))
                updateState { copy(loading = false, viewAction = viewAction) }
            } else {
                checkAuthenticator(info)
            }
        }
    }

    private suspend fun checkAuthenticator(info: SafeInfo) {
        var authenticator: AuthenticatorInfo? = null
        for (owner in info.owners) {
            authenticator = nullOnThrow { safeRepository.loadAuthenticatorInfo(owner) }
            if (authenticator != null) break
        }
        val viewAction =
            if (authenticator != null) {
                ViewAction.StartActivity(SetupNewRecoveryPhraseActivity.createIntent(context, safe, authenticator.address))
            } else {
                ViewAction.StartActivity(ScanExtensionAddressActivity.createIntent(context, safe))
            }
        updateState { copy(loading = false, viewAction = viewAction) }
    }

    override fun initialState() = State(false, null)

}

class SetupNewRecoveryPhraseIntroActivity : RecoveryPhraseIntroActivity() {

    @Inject
    lateinit var viewModel: SetupNewRecoveryPhraseIntroContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewComponent().inject(this)
        val safe = nullOnThrow { intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.let { it.asEthereumAddress()!! } }
            ?: run { finish(); return }
        viewModel.setup(safe)
        layout_recovery_phrase_intro_step_indicator.visible(false)
        layout_recovery_phrase_intro_next.isEnabled = false
        viewModel.state.observe(this, Observer {
            layout_recovery_phrase_intro_next.isEnabled = !it.loading
            layout_recovery_phrase_intro_next.handleViewAction(it.viewAction) { finish() }
        })
    }

    override fun onNextClicked() {
        viewModel.performNext()
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createIntent(context: Context, safeAddress: Solidity.Address) =
            Intent(context, SetupNewRecoveryPhraseIntroActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
            }
    }
}
