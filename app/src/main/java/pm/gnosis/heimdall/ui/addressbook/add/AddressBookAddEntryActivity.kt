package pm.gnosis.heimdall.ui.addressbook.add

import android.content.Context
import android.content.Intent
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
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.parseEthereumAddress
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class AddressBookAddEntryActivity : ViewModelActivity<AddressBookContract>() {
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.ADDRESS_BOOK_ENTRY

    override fun onStart() {
        super.onStart()
        disposables += toolbarHelper.setupShadow(layout_address_book_update_entry_toolbar_shadow, layout_address_book_update_entry_scroll_view)

        disposables += layout_address_book_update_entry_scan.clicks()
            .subscribeBy(onNext = { QRCodeScanActivity.startForResult(this) })

        disposables += layout_address_book_update_entry_save.clicks()
            .flatMap {
                viewModel.addAddressBookEntry(
                    layout_address_book_update_entry_address.text.toString(),
                    layout_address_book_update_entry_name.text.toString()
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onAddressBookEntryAdded, onError = ::onAddressBookEntryAddError)

        disposables += layout_address_book_update_entry_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)
    }

    private fun onAddressBookEntryAdded(entry: AddressBookEntry) {
        finish()
    }

    private fun onAddressBookEntryAddError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_address_book_update_entry_coordinator, throwable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        @Suppress("MoveLambdaOutsideParentheses")
        handleQrCodeActivityResult(requestCode, resultCode, data,
            {
                parseEthereumAddress(it)?.let {
                    layout_address_book_update_entry_address.setText(it.asEthereumAddressString())
                } ?: run {
                    snackbar(layout_address_book_update_entry_coordinator, R.string.invalid_ethereum_address)
                }
            })
    }

    override fun layout() = R.layout.layout_address_book_update_entry

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        fun createIntent(context: Context) = Intent(context, AddressBookAddEntryActivity::class.java)
    }
}
