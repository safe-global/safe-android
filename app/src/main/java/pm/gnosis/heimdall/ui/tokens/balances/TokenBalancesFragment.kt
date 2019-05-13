package pm.gnosis.heimdall.ui.tokens.balances

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_token_balances.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.tokens.manage.ManageTokensActivity
import pm.gnosis.heimdall.ui.transactions.create.CreateAssetTransferActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class TokenBalancesFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: TokenBalancesContract

    @Inject
    lateinit var adapter: TokenBalancesAdapter

    @Inject
    lateinit var eventTracker: EventTracker

    private lateinit var safeAddress: Solidity.Address

    private var trackingEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trackingEnabled = arguments?.getBoolean(ARGUMENT_TRACKING_ENABLED) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_token_balances, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        safeAddress = arguments?.getString(ARGUMENT_ADDRESS)?.asEthereumAddress()!!
        viewModel.setup(safeAddress)

        val layoutManager = LinearLayoutManager(context)
        ViewCompat.setNestedScrollingEnabled(layout_token_balances_list, false)
        layout_token_balances_list.layoutManager = layoutManager
        layout_token_balances_list.adapter = adapter
        layout_token_balances_list.addItemDecoration(DividerItemDecoration(context, layoutManager.orientation))
    }

    override fun onStart() {
        super.onStart()
        if (userVisibleHint && trackingEnabled) eventTracker.submit(Event.ScreenView(ScreenId.SAFE_ASSETS_VIEW))

        disposables += viewModel.observeLoadingStatus()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                layout_tokens_swipe_refresh.isRefreshing = it
            }, onError = Timber::e)

        disposables += viewModel.observeTokens(layout_tokens_swipe_refresh.refreshes())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onTokensList, onError = ::onTokensListError)

        disposables += adapter.tokenSelectedSubject
            .subscribe({ (token, _) ->
                startActivity(CreateAssetTransferActivity.createIntent(context!!, safeAddress, token))
            }, Timber::e)

        disposables += layout_token_balances_missing.clicks()
            .subscribeBy(onNext = {
                startActivity(ManageTokensActivity.createIntent(context!!))
            }, onError = Timber::e)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (::eventTracker.isInitialized && userVisibleHint && trackingEnabled) eventTracker.submit(Event.ScreenView(ScreenId.SAFE_ASSETS_VIEW))
    }

    private fun onTokensList(tokens: Adapter.Data<ERC20TokenWithBalance>) {
        adapter.updateData(tokens)
    }

    private fun onTokensListError(throwable: Throwable) {
        errorSnackbar(layout_tokens_coordinator_layout, throwable)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(context!!))
            .build().inject(this)
    }

    companion object {
        private const val ARGUMENT_ADDRESS = "argument.string.address"
        private const val ARGUMENT_TRACKING_ENABLED = "argument.boolean.tracking_enabled"

        fun createInstance(address: Solidity.Address, trackingEnabled: Boolean) =
            TokenBalancesFragment().withArgs(Bundle().apply {
                putString(ARGUMENT_ADDRESS, address.asEthereumAddressString())
                putBoolean(ARGUMENT_TRACKING_ENABLED, trackingEnabled)
            })
    }
}
