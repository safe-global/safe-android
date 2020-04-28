package io.gnosis.safe.helpers

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.BaseActivity
import io.gnosis.safe.ui.base.BaseFragment
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.utils.handleAddressBookResult
import io.gnosis.safe.utils.handleQrCodeActivityResult
import io.gnosis.safe.utils.parseEthereumAddress
import kotlinx.android.synthetic.main.bottom_sheet_address_input.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.exceptions.InvalidAddressException

class AddressInputHelper(
    fragment: BaseFragment<*>,
    private val addressCallback: (Solidity.Address) -> Unit,
    private val errorCallback: ((Throwable) -> Unit)? = null,
    allowAddressBook: Boolean = false
) {

    private val dialog =
        BottomSheetDialog(fragment.context!!).apply {
            val clipboard = fragment.activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            setContentView(layoutInflater.inflate(R.layout.bottom_sheet_address_input, null))
            bottom_sheet_address_input_book.visible(allowAddressBook)
            bottom_sheet_address_input_book_icon.visible(allowAddressBook)
            bottom_sheet_address_input_book_touch.visible(allowAddressBook)
            if (allowAddressBook) {
                bottom_sheet_address_input_book_touch.setOnClickListener {
                    //TODO uncomment when address book functionality is ready
//                    activity.selectFromAddressBook()
                    hide()
                }
            }
            bottom_sheet_address_input_ens_touch.setOnClickListener {
                EnsInputDialog.create().apply {
                    callback = addressCallback
                    show(fragment.childFragmentManager, null)
                }
                hide()
            }
            bottom_sheet_address_input_qr_touch.setOnClickListener {
                QRCodeScanActivity.startForResult(fragment)
                hide()
            }
            bottom_sheet_address_input_paste_touch.setOnClickListener {
                (clipboard.primaryClip?.getItemAt(0)?.text?.let { parseEthereumAddress(it.toString()) }
                    ?: run {
                        handleError(IllegalArgumentException("No Ethereum address found"))
                        null
                    })?.let { addressCallback(it) }
                hide()
            }
        }

    fun showDialog() = dialog.show()

    fun hideDialog() = dialog.hide()

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("MoveLambdaOutsideParentheses")
        if (!handleQrCodeActivityResult(requestCode, resultCode, data, {
                addressCallback(parseEthereumAddress(it) ?: run {
                    handleError(InvalidAddressException(it))
                    return@handleQrCodeActivityResult
                })
            })) {
            @Suppress("MoveLambdaOutsideParentheses")
            handleAddressBookResult(requestCode, resultCode, data, {
                addressCallback(it.address)
            })
        }
    }

    private fun handleError(t: Throwable) {
        errorCallback?.invoke(t) ?: dialog.context.toast(R.string.invalid_ethereum_address)
    }
}
