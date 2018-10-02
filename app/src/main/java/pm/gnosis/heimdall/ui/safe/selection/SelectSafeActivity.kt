package pm.gnosis.heimdall.ui.safe.selection

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_select_safe.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.base.SimpleSpinnerAdapter
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import timber.log.Timber
import javax.inject.Inject

class SelectSafeActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: SelectSafeContract

    private val transaction by lazy {
        intent?.getParcelableExtra<SafeTransaction>(EXTRA_TRANSACTION)
    }

    private val adapter by lazy {
        // Adapter should only be created if we need it
        SafesSpinnerAdapter(this)
    }

    override fun screenId() = ScreenId.SELECT_SAFE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_select_safe)
        setupToolbar(layout_select_safe_toolbar, R.drawable.ic_close_24dp)
        layout_select_safe_spinner.adapter = adapter
        transaction ?: finish()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this])
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.loadSafes()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::setSpinnerData, Timber::e)

        disposables += layout_select_safe_review_button.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle {
                val safe = nullOnThrow { adapter.getItem(layout_select_safe_spinner.selectedItemPosition) }
                viewModel.reviewTransaction(safe?.address, transaction!!)
            }
            .subscribeForResult(::startActivity) { errorSnackbar(layout_select_safe_review_button, it) }
    }

    private fun setSpinnerData(data: List<Safe>) {
        adapter.clear()
        adapter.addAll(data)
        adapter.notifyDataSetChanged()
    }

    private class SafesSpinnerAdapter(context: Context) : SimpleSpinnerAdapter<Safe>(context) {
        override fun title(item: Safe): String? =
            null // TODO: fix if this activity is used again

        override fun subTitle(item: Safe) =
            item.address.asEthereumAddressString()
    }

    companion object {

        private const val EXTRA_TRANSACTION = "extra.parcelable.wrapped"

        fun createIntent(context: Context, transaction: SafeTransaction) =
            Intent(context, SelectSafeActivity::class.java).apply {
                putExtra(EXTRA_TRANSACTION, transaction)
            }
    }
}
