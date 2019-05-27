package pm.gnosis.heimdall.ui.addressbook.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_book_update_entry.*
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.AddressInputHelper
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.dialogs.ens.EnsInputDialog
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.parseEthereumAddress
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class AddressBookAddEntryActivity : ViewModelActivity<AddressBookContract>() {
    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private lateinit var addressInputHelper: AddressInputHelper

    override fun screenId() = ScreenId.ADDRESS_BOOK_ENTRY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addressInputHelper = AddressInputHelper(this, ::updateAddress, allowAddressBook = false)
    }

    override fun onStart() {
        super.onStart()

        layout_address_book_update_entry_address.inputType = InputType.TYPE_NULL
        layout_address_book_update_entry_address.keyListener = null
        disposables += Observable.merge(
            layout_address_book_update_entry_address_input_layout.clicks(),
            layout_address_book_update_entry_address.clicks()
        )
            .subscribeBy(onNext = {
                addressInputHelper.showDialog()
            })

        disposables += toolbarHelper.setupShadow(layout_address_book_update_entry_toolbar_shadow, layout_address_book_update_entry_scroll_view)

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

    private fun updateAddress(address: Solidity.Address) {
        layout_address_book_update_entry_address.setText(address.asEthereumAddressString())
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
        addressInputHelper.handleResult(requestCode, resultCode, data)
    }

    override fun layout() = R.layout.layout_address_book_update_entry

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        fun createIntent(context: Context) = Intent(context, AddressBookAddEntryActivity::class.java)
    }
}
