package pm.gnosis.heimdall.ui.addressbook.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_book.*
import kotlinx.android.synthetic.main.layout_address_book_add_entry.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.scanQrCode
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.common.utils.toast
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class AddressBookAddEntryActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: AddressBookContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_address_book_add_entry)
        registerToolbar(layout_add_address_book_entry_toolbar)
        layout_add_address_book_entry_toolbar.title = getString(R.string.add_address)
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_add_address_book_entry_scan.clicks()
                .subscribeBy(onNext = { scanQrCode() })

        disposables += layout_add_address_book_entry_address.textChanges()
                .subscribeBy(onNext = { layout_add_address_book_entry_address_container.error = null })

        disposables += layout_add_address_book_entry_name.textChanges()
                .subscribeBy(onNext = { layout_add_address_book_entry_name_container.error = null })

        disposables += layout_add_address_book_entry_save.clicks()
                .flatMap {
                    viewModel.addAddressBookEntry(
                            layout_add_address_book_entry_address.text.toString(),
                            layout_add_address_book_entry_name.text.toString(),
                            layout_add_address_book_entry_description.text.toString())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = ::onAddressBookEntryAdded, onError = ::onAddressBookEntryAddError)
    }

    private fun onAddressBookEntryAdded(entry: AddressBookEntry) {
        toast("Added ${entry.name}")
        finish()
    }

    private fun onAddressBookEntryAddError(throwable: Throwable) {
        Timber.e(throwable)
        when (throwable) {
            is InvalidAddressException -> layout_add_address_book_entry_address_container.error = getString(R.string.invalid_ethereum_address)
            is AddressBookContract.NameIsBlankException -> layout_add_address_book_entry_name_container.error = getString(R.string.name_cannot_be_blank)
            is AddressBookContract.AddressAlreadyAddedException -> layout_add_address_book_entry_address_container.error = getString(R.string.address_already_in_address_book)
            else -> snackbar(layout_add_address_book_entry_coordinator, getString(R.string.unknown_error))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data,
                {
                    if (it.isValidEthereumAddress()) {
                        layout_add_address_book_entry_address.setText(it)
                    } else {
                        snackbar(layout_address_book_coordinator, R.string.invalid_ethereum_address)
                    }

                }, { snackbar(layout_address_book_coordinator, R.string.qr_code_scan_cancel) })
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, AddressBookAddEntryActivity::class.java)
    }
}
