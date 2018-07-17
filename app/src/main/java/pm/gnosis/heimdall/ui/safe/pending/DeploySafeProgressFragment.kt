package pm.gnosis.heimdall.ui.safe.pending

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_deploy_safe_progress.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.setupEtherscanTransactionUrl
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.asTransactionHash
import timber.log.Timber
import javax.inject.Inject

class DeploySafeProgressFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: DeploySafeProgressContract

    @Inject
    lateinit var eventTracker: EventTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_deploy_safe_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layout_deploy_safe_progress_title.text = SpannableStringBuilder("")
            .append(getString(R.string.deposit_received))
            .append("\n")
            .appendText(getString(R.string.creating_your_new_safe), StyleSpan(Typeface.BOLD))

        arguments?.getString(EXTRA_SAFE_TX_HASH)?.let {
            layout_deploy_safe_progress_description.setupEtherscanTransactionUrl(it, R.string.deploy_safe_description)
        }
    }

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.NEW_SAFE_AWAIT_DEPLOYMENT))

        disposables += viewModel.notifySafeFunded()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::onDeployedSafe, onError = Timber::e)
    }

    private fun onDeployedSafe(address: Solidity.Address) {
        startActivity(SafeMainActivity.createIntent(context!!, address))
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(component)
            .build().inject(this)
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        private const val EXTRA_SAFE_TX_HASH = "extra.string.safe_tx_hash"

        fun createInstance(pendingSafe: PendingSafe) = DeploySafeProgressFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, pendingSafe.address.asEthereumAddressString())
            putString(EXTRA_SAFE_TX_HASH, pendingSafe.hash.asTransactionHash())
        })
    }
}
