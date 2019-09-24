package pm.gnosis.heimdall.ui.addressbook.list

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_book.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.addressbook.add.AddressBookAddEntryActivity
import pm.gnosis.heimdall.ui.addressbook.detail.AddressBookEntryDetailsActivity
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class AddressBookActivity : ViewModelActivity<AddressBookContract>() {

    override fun screenId() = ScreenId.ADDRESS_BOOK

    @Inject
    lateinit var adapter: AddressBookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutManager = LinearLayoutManager(this)
        layout_address_book_list.layoutManager = layoutManager
        layout_address_book_list.adapter = adapter
        layout_address_book_list.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_address_book_add
            .clicks()
            .subscribeBy(onNext = { startActivity(AddressBookAddEntryActivity.createIntent(this)) })

        disposables += viewModel.observeAddressBook()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onAddressBook, onError = ::onAddressBookError)

        disposables += adapter.clicks
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::handleEntryClick, onError = Timber::e)

        disposables += layout_address_book_entry_details_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)
    }

    private fun onAddressBook(adapterData: Adapter.Data<AddressBookEntry>) {
        layout_address_book_empty_view.visibility = if (adapterData.entries.isEmpty()) View.VISIBLE else View.GONE
        adapter.updateData(adapterData)
    }

    private fun onAddressBookError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_address_book_coordinator, R.string.error_try_again)
    }

    private fun handleEntryClick(entry: AddressBookEntry) {
        callingActivity?.apply {
            // Was started with startActivityForResult therefore we return the selected entry
            setResult(Activity.RESULT_OK, createResult(entry))
            finish()
        } ?: run {
            startActivity(AddressBookEntryDetailsActivity.createIntent(this, entry.address))
        }
    }

    override fun layout() = R.layout.layout_address_book

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        const val REQUEST_CODE = 0x00001337 // Only use bottom 16 bits
        private const val RESULT_ENTRY_NAME = "result.string.entry_name"
        private const val RESULT_ENTRY_ADDRESS = "result.string.entry_address"
        private const val RESULT_ENTRY_DESCRIPTION = "result.string.entry_description"

        private fun createResult(entry: AddressBookEntry): Intent =
            Intent().apply {
                putExtra(RESULT_ENTRY_ADDRESS, entry.address.asEthereumAddressString())
                putExtra(RESULT_ENTRY_NAME, entry.name)
                putExtra(RESULT_ENTRY_DESCRIPTION, entry.description)
            }

        fun parseResult(intent: Intent?): AddressBookEntry? {
            intent ?: return null
            val address = intent.getStringExtra(RESULT_ENTRY_ADDRESS)?.asEthereumAddress() ?: return null
            val name = intent.getStringExtra(RESULT_ENTRY_NAME) ?: return null
            val description = intent.getStringExtra(RESULT_ENTRY_DESCRIPTION) ?: return null
            return AddressBookEntry(address, name, description)
        }

        fun createIntent(context: Context) = Intent(context, AddressBookActivity::class.java)
    }
}
