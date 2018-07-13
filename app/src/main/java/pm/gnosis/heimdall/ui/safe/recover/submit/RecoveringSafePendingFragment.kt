package pm.gnosis.heimdall.ui.safe.recover.submit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_recovering_safe_pending.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.setupEtherscanTransactionUrl
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.asTransactionHash
import javax.inject.Inject

class RecoveringSafePendingFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: RecoveringSafeContract

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_recovering_safe_pending, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(EXTRA_SAFE_TX_HASH)?.let {
            layout_recovering_safe_pending_description.setupEtherscanTransactionUrl(it, R.string.deploy_safe_description)
        }
    }

    override fun onStart() {
        super.onStart()
        val safeAddress = arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: throw IllegalStateException()
        disposables += viewModel.checkRecoveryStatus(safeAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = ::onDeployedSafe, onError = {
                errorSnackbar(layout_recovering_safe_pending_description, it)
            })
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

        fun createInstance(safe: RecoveringSafe) = RecoveringSafePendingFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
            putString(EXTRA_SAFE_TX_HASH, safe.transactionHash?.asTransactionHash())
        })
    }
}
