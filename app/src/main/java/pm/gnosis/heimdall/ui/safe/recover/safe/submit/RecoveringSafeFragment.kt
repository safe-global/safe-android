package pm.gnosis.heimdall.ui.safe.recover.safe.submit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_recovering_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.di.components.ApplicationComponent
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.transaction
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class RecoveringSafeFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: RecoveringSafeContract

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder().applicationComponent(component).viewModule(ViewModule(context!!)).build().inject(this)
    }

    override fun onStart() {
        super.onStart()
        childFragmentManager.findFragmentByTag(FRAGMENT_TAG)?.let { childFragmentManager.transaction { remove(it) } }
        layout_recovering_safe_progress.visible(true)
        val safeAddress = arguments?.getString(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: throw IllegalStateException("No safe address provided!")
        disposables += viewModel.checkSafeState(safeAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = ::handleError) { (safe, state) ->
                layout_recovering_safe_progress.visible(false)
                when (state) {
                    RecoveringSafeContract.RecoveryState.ERROR -> {
                        // TODO: implement proper screen once design is available
                        snackbar(layout_recovering_safe_container, R.string.recovery_error)
                        null
                    }
                    RecoveringSafeContract.RecoveryState.CREATED -> {
                        RecoveringSafeFundFragment.createInstance(safe)
                    }
                    RecoveringSafeContract.RecoveryState.FUNDED -> {
                        RecoveringSafeSubmitFragment.createInstance(safe)
                    }
                    RecoveringSafeContract.RecoveryState.PENDING -> {
                        RecoveringSafePendingFragment.createInstance(safe)
                    }
                }?.let {
                    childFragmentManager.transaction {
                        replace(R.id.layout_recovering_safe_container, it,
                            FRAGMENT_TAG
                        )
                    }
                }
            }
    }

    private fun handleError(throwable: Throwable) {
        errorSnackbar(layout_recovering_safe_container, throwable)
        // TODO: enable retry
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_recovering_safe, container, false)

    companion object {
        private const val FRAGMENT_TAG = "tag.fragment.recovering_safe_child"
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"

        fun createInstance(safe: RecoveringSafe) = RecoveringSafeFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_ADDRESS, safe.address.asEthereumAddressString())
        })
    }
}
