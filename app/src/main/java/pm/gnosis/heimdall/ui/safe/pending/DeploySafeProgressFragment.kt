package pm.gnosis.heimdall.ui.safe.pending

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_deploy_safe_progress.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.setupEtherscanTransactionUrl
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asTransactionHash
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import timber.log.Timber
import javax.inject.Inject

class DeploySafeProgressFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: DeploySafeProgressContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i(arguments?.getString(EXTRA_SAFE_TX_HASH))
        viewModel.setup(arguments?.getString(EXTRA_SAFE_TX_HASH)?.hexAsBigInteger())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_deploy_safe_progress, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layout_deploy_safe_progress_title.text = SpannableStringBuilder("")
            .append(getString(R.string.deposit_received))
            .append("\n")
            .appendText(getString(R.string.creating_your_new_safe), StyleSpan(Typeface.BOLD))

        viewModel.getTransactionHash()?.toHexString()?.let {
            layout_deploy_safe_progress_description.setupEtherscanTransactionUrl(it, R.string.deploy_safe_description)
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.notifySafeFunded()
            .subscribeBy(onSuccess = ::onDeployedSafe, onError = Timber::e)
    }

    private fun onDeployedSafe(address: Solidity.Address) {
        startActivity(SafeMainActivity.createIntent(context!!, address.value))
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(component)
            .build().inject(this)
    }

    companion object {
        private const val EXTRA_SAFE_TX_HASH = "extra.string.safe_tx_hash"

        fun createInstance(pendingSafe: PendingSafe) = DeploySafeProgressFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_TX_HASH, pendingSafe.hash.asTransactionHash())
        })
    }
}
