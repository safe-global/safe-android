package pm.gnosis.heimdall.ui.two_factor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import kotlinx.android.synthetic.main.layout_select_authenticator.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.modules.ApplicationModule
import pm.gnosis.heimdall.ui.base.BaseStateViewModel
import pm.gnosis.heimdall.ui.base.handleViewAction
import pm.gnosis.heimdall.ui.transactions.view.review.ReviewTransactionActivity
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import javax.inject.Inject

abstract class ConnectAuthenticatorContract(context: Context, appDispatcher: ApplicationModule.AppCoroutineDispatchers) :
    BaseStateViewModel<ConnectAuthenticatorContract.State>(context, appDispatcher) {

    abstract fun setup(safe: Solidity.Address)

    abstract fun handleAuthenticatorInfo(info: AuthenticatorSetupInfo)

    data class State(override var viewAction: ViewAction?) : BaseStateViewModel.State
}

class ConnectAuthenticatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    appDispatcher: ApplicationModule.AppCoroutineDispatchers,
    private val safeRepository: GnosisSafeRepository
) : ConnectAuthenticatorContract(context, appDispatcher) {

    override val state = liveData {
        for (event in stateChannel.openSubscription()) emit(event)
    }

    private lateinit var safe: Solidity.Address
    override fun setup(safe: Solidity.Address) {
        this.safe = safe
    }

    override fun handleAuthenticatorInfo(info: AuthenticatorSetupInfo) {
        safeLaunch {
            safeRepository.saveAuthenticatorInfo(info.authenticator)
            updateState(true) {
                apply {
                    viewAction = ViewAction.StartActivity(
                        /*TODO: adjust layout of review transaction ativity
                                use PairinSubmitActivity for pairing transactions
                        */
                        ReviewTransactionActivity.createIntent(
                            context = context,
                            safe = safe,
                            txData = TransactionData.ConnectAuthenticator(info.authenticator.address)
                        )
                    )
                }
            }
        }
    }

    override fun initialState() = State(null)

}

class Connect2FaActivity : Select2FaActivity() {
    @Inject
    lateinit var viewModel: ConnectAuthenticatorContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewComponent().inject(this)
        viewModel.setup(getSelectAuthenticatorExtras()!!)
        viewModel.state.observe(this, Observer {
            select_authenticator_scroll.handleViewAction(it.viewAction) { finish() }
        })
    }

    override fun onAuthenticatorSetupInfo(info: AuthenticatorSetupInfo) {
        viewModel.handleAuthenticatorInfo(info)
    }

    companion object {
        fun createIntent(context: Context, safe: Solidity.Address) =
            Intent(context, Connect2FaActivity::class.java).addSelectAuthenticatorExtras(safe)
    }
}
