package pm.gnosis.heimdall.ui.safe.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.svalinn.common.utils.withArgs
import pm.gnosis.utils.asTransactionHash
import pm.gnosis.utils.hexAsBigInteger
import timber.log.Timber
import javax.inject.Inject


class PendingSafeFragment : BaseFragment() {

    @Inject
    lateinit var safeRepository: GnosisSafeRepository

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder().applicationComponent(component).viewModule(ViewModule(context!!)).build().inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.layout_pending_safe, container, false)

    override fun onStart() {
        super.onStart()
        disposables += safeRepository.observeDeployStatus(arguments?.getString(EXTRA_SAFE_TX_HASH) ?: "")
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = {
                startActivity(SafeMainActivity.createIntent(context!!, it.hexAsBigInteger()))
            }, onError = Timber::e)
    }

    companion object {
        private const val EXTRA_SAFE_NAME = "extra.string.safe_name"
        private const val EXTRA_SAFE_TX_HASH = "extra.string.safe_tx_hash"

        fun createInstance(safe: PendingSafe) = PendingSafeFragment().withArgs(Bundle().apply {
            putString(EXTRA_SAFE_NAME, safe.name)
            putString(EXTRA_SAFE_TX_HASH, safe.hash.asTransactionHash())
        })
    }
}
