package pm.gnosis.heimdall.ui.addressbook.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_book_update_entry.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class AddressBookEditEntryActivity : ViewModelActivity<AddressBookContract>() {
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun layout() = R.layout.layout_address_book_update_entry

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.ADDRESS_BOOK_EDIT_ENTRY

    private lateinit var address: Solidity.Address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra(EXTRA_ADDRESS).let {
            it.asEthereumAddress()?.let {
                address = it
            } ?: run {
                toast(R.string.invalid_ethereum_address)
                finish()
                return
            }
        }

        layout_address_book_update_entry_scan.visibility = View.GONE
        layout_address_book_update_entry_address.isEnabled = false
        layout_address_book_update_entry_address.setTextColor(getColorCompat(R.color.bluey_grey))
    }

    override fun onStart() {
        super.onStart()
        disposables += toolbarHelper.setupShadow(layout_address_book_update_entry_toolbar_shadow, layout_address_book_update_entry_scroll_view)

        disposables += viewModel.loadAddressBookEntry(address)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                layout_address_book_update_entry_name.setText(it.name)
                layout_address_book_update_entry_address.setText(it.address.asEthereumAddressString())
            }, onError = {
                Timber.e(it)
                finish()
            })

        disposables += layout_address_book_update_entry_save.clicks()
            .flatMap { viewModel.updateAddressBookEntry(address, layout_address_book_update_entry_name.text.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onAddressBookEntryUpdated, onError = ::onAddressBookEntryUpdateError)

        disposables += layout_address_book_update_entry_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)
    }


    private fun onAddressBookEntryUpdated(entry: AddressBookEntry) {
        finish()
    }

    private fun onAddressBookEntryUpdateError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_address_book_update_entry_coordinator, throwable)
    }

    companion object {
        private const val EXTRA_ADDRESS = "extra.string.address"

        fun createIntent(context: Context, address: Solidity.Address) = Intent(context, AddressBookEditEntryActivity::class.java).apply {
            putExtra(EXTRA_ADDRESS, address.asEthereumAddressString())
        }
    }
}
