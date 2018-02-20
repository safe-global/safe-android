package pm.gnosis.heimdall.ui.addressbook.list

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_address_book.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.addressbook.add.AddressBookAddEntryActivity
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.models.AddressBookEntry
import timber.log.Timber
import javax.inject.Inject

class AddressBookActivity : BaseActivity() {

    override fun screenId() = ScreenId.ADDRESS_BOOK

    @Inject
    lateinit var viewModel: AddressBookContract

    @Inject
    lateinit var adapter: AddressBookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_address_book)
        inject()

        registerToolbar(layout_address_book_toolbar)
        layout_address_book_toolbar.title = getString(R.string.address_book)

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
    }

    private fun onAddressBook(adapterData: Adapter.Data<AddressBookEntry>) {
        layout_address_book_empty_view.visibility = if (adapterData.entries.isEmpty()) View.VISIBLE else View.GONE
        adapter.updateData(adapterData)
    }

    private fun onAddressBookError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_address_book_coordinator, R.string.error_try_again)
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, AddressBookActivity::class.java)
    }
}
