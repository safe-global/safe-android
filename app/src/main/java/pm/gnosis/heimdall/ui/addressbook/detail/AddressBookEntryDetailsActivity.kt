package pm.gnosis.heimdall.ui.addressbook.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.jakewharton.rxbinding2.support.v7.widget.itemClicks
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_address_book_entry_details.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.addressbook.edit.AddressBookEditEntryActivity
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.dialogs.share.SimpleAddressShareDialog
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber

class AddressBookEntryDetailsActivity : ViewModelActivity<AddressBookContract>() {

    override fun screenId() = ScreenId.ADDRESS_BOOK_ENTRY_DETAILS

    lateinit var address: Solidity.Address

    private val deleteEntryClickSubject = PublishSubject.create<Unit>()

    private lateinit var popupMenu: PopupMenu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra(EXTRA_ADDRESS_ENTRY).let {
            it.asEthereumAddress()?.let {
                address = it
            } ?: run {
                toast(R.string.invalid_ethereum_address)
                finish()
                return
            }
        }

        popupMenu = PopupMenu(this, layout_address_book_entry_details_overflow).apply {
            inflate(R.menu.address_book_entry_details_menu)
            menu.findItem(R.id.address_book_entry_details_menu_delete).title = SpannableStringBuilder().appendText(
                getString(R.string.delete), ForegroundColorSpan(getColorCompat(R.color.tomato))
            )
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeAddressBookEntry(address)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onAddressBookEntry, onError = Timber::e)

        disposables += deleteEntryClickSubject
            .flatMap { viewModel.deleteAddressBookEntry(address) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onAddressBookEntryDeleted, onError = ::onAddressBookEntryDeleteError)

        disposables += layout_address_book_entry_details_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        disposables += layout_address_book_entry_details_scan.clicks()
            .subscribeBy(onNext = {
                SimpleAddressShareDialog.create(address.asEthereumAddressString()).show(supportFragmentManager, null)
            }, onError = Timber::e)

        disposables += layout_address_book_entry_details_overflow.clicks()
            .subscribeBy(onNext = { popupMenu.show() }, onError = Timber::e)

        disposables += popupMenu.itemClicks()
            .subscribeBy(onNext = {
                when (it.itemId) {
                    R.id.address_book_entry_details_menu_delete -> showDeleteDialog()
                }
            }, onError = Timber::e)

        disposables += layout_address_book_entry_details_edit.clicks()
            .subscribeBy(onNext = {
                startActivity(AddressBookEditEntryActivity.createIntent(this, address))
            }, onError = Timber::e)
    }

    private fun onAddressBookEntry(entry: AddressBookEntry) {
        layout_address_book_entry_details_title.text = entry.name
        layout_address_book_entry_details_name.text = entry.name
        layout_address_book_entry_details_address.text = entry.address.asEthereumAddressString()
    }

    private fun onAddressBookEntryDeleted(address: Solidity.Address) {
        toast(getString(R.string.toast_delete_address_book_entry, address.asEthereumAddressString()))
        finish()
    }

    private fun onAddressBookEntryDeleteError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_address_book_entry_details_coordinator, throwable, defaultErrorMsg = R.string.address_book_entry_delete_error)
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.dialog_delete_address_entry_message))
            .setPositiveButton(R.string.delete) { _, _ -> deleteEntryClickSubject.onNext(Unit) }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    override fun layout() = R.layout.layout_address_book_entry_details

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        private const val EXTRA_ADDRESS_ENTRY = "extra.string.address_entry"

        fun createIntent(context: Context, address: Solidity.Address) =
            Intent(context, AddressBookEntryDetailsActivity::class.java)
                .apply { putExtra(EXTRA_ADDRESS_ENTRY, address.asEthereumAddressString()) }
    }
}
