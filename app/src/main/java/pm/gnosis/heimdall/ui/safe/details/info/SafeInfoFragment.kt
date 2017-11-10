package pm.gnosis.heimdall.ui.safe.details.info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_safe_info.*
import kotlinx.android.synthetic.main.layout_safe_owner.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.build
import pm.gnosis.heimdall.common.utils.getSimplePlural
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.withArgs
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.stringWithNoTrailingZeroes
import javax.inject.Inject


class SafeInfoFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: SafeInfoContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setup(arguments!!.getString(ARGUMENT_SAFE_ADDRESS).hexAsBigInteger())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = layoutInflater?.inflate(R.layout.layout_safe_info, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        disposables += layout_safe_info_swipe_refresh.refreshes()
                .map { true }
                .startWith(false)
                .flatMap {
                    viewModel.loadSafeInfo(it)
                            .subscribeOn(AndroidSchedulers.mainThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe { showLoading(true) }
                            .doOnComplete { showLoading(false) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(this::updateInfo, this::handleError)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(context!!))
                .build().inject(this)
    }

    private fun showLoading(loading: Boolean) {
        layout_safe_info_swipe_refresh.isRefreshing = loading
    }

    private fun handleError(throwable: Throwable) {
        view?.let { errorSnackbar(it, throwable) }
    }

    private fun updateInfo(info: SafeInfo) {
        layout_safe_info_balance.text = getString(R.string.x_ether, info.balance.toEther().stringWithNoTrailingZeroes())
        layout_safe_info_confirmations.text = context!!.getSimplePlural(R.plurals.x_confirmations, info.requiredConfirmations)

        setupOwners(info.owners)
    }

    private fun setupOwners(owners: List<String>) {
        for (i in layout_safe_info_owners_container.childCount - 1 downTo 1) {
            layout_safe_info_owners_container.removeViewAt(i)
        }
        owners.forEach { addOwner(it) }
    }

    private fun addOwner(address: String) {
        if (layout_safe_info_owners_container.childCount > 1) {
            val divider = layoutInflater.inflate(R.layout.layout_list_divider, layout_safe_info_owners_container, false)
            layout_safe_info_owners_container.addView(divider)
        }
        val ownerLayout = layoutInflater.inflate(R.layout.layout_safe_owner, layout_safe_info_owners_container, false)
        ownerLayout.layout_safe_owner_address.text = address
        layout_safe_info_owners_container.addView(ownerLayout)
    }

    companion object {
        private const val ARGUMENT_SAFE_ADDRESS = "argument.string.safe_address"

        fun createInstance(address: String) =
                SafeInfoFragment().withArgs(
                        Bundle().build { putString(ARGUMENT_SAFE_ADDRESS, address) }
                )
    }
}
