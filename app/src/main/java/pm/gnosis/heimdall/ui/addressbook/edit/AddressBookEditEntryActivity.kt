package pm.gnosis.heimdall.ui.addressbook.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.parseEthereumAddress
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.exceptions.InvalidAddressException
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

        // Needs to be done on onCreate because the result of the QR Scan activity would get "overridden" by the DB load
        disposables += viewModel.loadAddressBookEntry(address)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                layout_address_book_update_entry_name.setText(it.name)
                layout_address_book_update_entry_address.setText(it.address.asEthereumAddressString())
            }, onError = {
                Timber.e(it)
                finish()
            })
    }

    override fun onStart() {
        super.onStart()
        disposables += toolbarHelper.setupShadow(layout_address_book_update_entry_toolbar_shadow, layout_address_book_update_entry_scroll_view)

        disposables += layout_address_book_update_entry_scan.clicks()
            .subscribeBy(onNext = { QRCodeScanActivity.startForResult(this) })

        disposables += layout_address_book_update_entry_save.clicks()
            .flatMap {
                viewModel.updateAddressBookEntry(
                    address,
                    layout_address_book_update_entry_name.text.toString(),
                    layout_address_book_update_entry_address.text.toString()
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onAddressBookEntryUpdated, onError = ::onAddressBookEntryUpdateError)

        disposables += layout_address_book_update_entry_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)
    }


    private fun onAddressBookEntryUpdated(entry: AddressBookEntry) {
        startActivity(AddressBookActivity.createIntent(this).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP })
    }

    private fun onAddressBookEntryUpdateError(throwable: Throwable) {
        Timber.e(throwable)
        toast(
            when (throwable) {
                is InvalidAddressException -> R.string.invalid_ethereum_address
                is AddressBookContract.NameIsBlankException -> R.string.name_cannot_be_blank
                is AddressBookContract.AddressAlreadyAddedException -> R.string.address_already_in_address_book
                else -> R.string.unknown_error
            }
        )
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

    companion object {
        private const val EXTRA_ADDRESS = "extra.string.address"

        fun createIntent(context: Context, address: Solidity.Address) = Intent(context, AddressBookEditEntryActivity::class.java).apply {
            putExtra(EXTRA_ADDRESS, address.asEthereumAddressString())
        }
    }
}
