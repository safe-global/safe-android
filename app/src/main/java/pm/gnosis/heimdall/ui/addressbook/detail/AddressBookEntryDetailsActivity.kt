package pm.gnosis.heimdall.ui.addressbook.detail

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_address_book_entry_details.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.ui.addressbook.AddressBookContract
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.utils.asEthereumAddressStringOrNull
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class AddressBookEntryDetailsActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: AddressBookContract

    lateinit var address: BigInteger

    private val generateQrCodeSubject = PublishSubject.create<Unit>()
    private val deleteEntryClickSubject = PublishSubject.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()

        (intent.extras.getSerializable(EXTRA_ADDRESS_ENTRY) as? BigInteger?).let {
            if (it == null || !it.isValidEthereumAddress()) {
                toast(R.string.invalid_ethereum_address)
                finish()
                return
            } else {
                address = it
            }
        }

        setContentView(R.layout.layout_address_book_entry_details)
        layout_address_book_entry_details_toolbar.inflateMenu(R.menu.address_book_entry_details_menu)
        layout_address_book_entry_details_toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        layout_address_book_entry_details_toolbar.setNavigationOnClickListener { onBackPressed() }
        layout_address_book_entry_details_toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.address_book_entry_details_menu_share ->
                    address.asEthereumAddressStringOrNull()?.let { shareExternalText(it, R.string.share_address) }
                R.id.address_book_entry_details_menu_delete -> showDeleteDialog()
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeAddressBookEntry(address)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onAddressBookEntry)

        disposables += generateQrCodeSubject
                .flatMap {
                    viewModel.generateQrCode(address, ContextCompat.getColor(this, R.color.window_background))
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe { layout_address_book_entry_details_qr_code_loading.visibility = View.VISIBLE }
                            .doOnTerminate { layout_address_book_entry_details_qr_code_loading.visibility = View.GONE }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onQrCodeGenerated, onError = this::onQrCodeGenerateError)

        disposables += deleteEntryClickSubject
                .flatMap {
                    Observable.just(ErrorResult<BigInteger>(Exception()))
                    //   viewModel.deleteAddressBookEntry(address)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onAddressBookEntryDeleted, onError = this::onAddressBookEntryDeleteError)
    }

    private fun onAddressBookEntry(entry: AddressBookEntry) {
        val address = entry.address.asEthereumAddressStringOrNull()
        if (address == null) {
            toast(R.string.invalid_ethereum_address)
            finish()
            return
        }
        generateQrCodeSubject.onNext(Unit)
        layout_address_book_entry_details_name.text = entry.name
        layout_address_book_entry_details_address.text = address
        layout_address_book_entry_details_description_container.visibility =
                if (entry.description.isBlank()) View.GONE else View.VISIBLE
        layout_address_book_entry_details_description.text = entry.description
    }

    private fun onQrCodeGenerated(bitmap: Bitmap) {
        layout_address_book_entry_details_qr_code.visibility = View.VISIBLE
        layout_address_book_entry_details_qr_code.setImageBitmap(bitmap)
    }

    private fun onQrCodeGenerateError(throwable: Throwable) {
        Timber.e(throwable)
        layout_address_book_entry_details_qr_code.visibility = View.INVISIBLE
        snackbar(layout_address_book_entry_details_coordinator, R.string.qr_code_error,
                duration = Snackbar.LENGTH_INDEFINITE,
                action = R.string.retry to { _: View -> generateQrCodeSubject.onNext(Unit) })
    }

    private fun onAddressBookEntryDeleted(address: BigInteger) {
        toast(getString(R.string.toast_delete_address_book_entry, address.asEthereumAddressStringOrNull()))
        finish()
    }

    private fun onAddressBookEntryDeleteError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_address_book_entry_details_coordinator, R.string.address_book_entry_delete_error)
    }

    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
                .setMessage(getString(R.string.dialog_delete_address_entry_message))
                .setPositiveButton(R.string.delete, { _, _ -> deleteEntryClickSubject.onNext(Unit) })
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .show()
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    companion object {
        private const val EXTRA_ADDRESS_ENTRY = "extra.string.address_entry"

        fun createIntent(context: Context, address: BigInteger) =
                Intent(context, AddressBookEntryDetailsActivity::class.java)
                        .apply { putExtra(EXTRA_ADDRESS_ENTRY, address) }
    }
}
