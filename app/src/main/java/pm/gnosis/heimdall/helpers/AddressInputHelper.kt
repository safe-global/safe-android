package pm.gnosis.heimdall.helpers

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_address_input.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.dialogs.ens.EnsInputDialog
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.handleAddressBookResult
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.parseEthereumAddress
import pm.gnosis.heimdall.utils.selectFromAddressBook
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.exceptions.InvalidAddressException

class AddressInputHelper(
    activity: BaseActivity,
    private val addressCallback: (Solidity.Address) -> Unit,
    private val errorCallback: ((Throwable) -> Unit)? = null,
    allowAddressBook: Boolean = true) {

    private val dialog =
        BottomSheetDialog(activity).apply {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            setContentView(layoutInflater.inflate(R.layout.bottom_sheet_address_input, null))
            bottom_sheet_address_input_book.visible(allowAddressBook)
            if (allowAddressBook) {
                bottom_sheet_address_input_book.setOnClickListener {
                    activity.selectFromAddressBook()
                    hide()
                }
            }
            bottom_sheet_address_input_ens.setOnClickListener {
                EnsInputDialog.create().apply {
                    callback = addressCallback
                    show(activity.supportFragmentManager, null)
                }
                hide()
            }
            bottom_sheet_address_input_qr.setOnClickListener {
                QRCodeScanActivity.startForResult(activity)
                hide()
            }
            bottom_sheet_address_input_paste.setOnClickListener {
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
        errorCallback?.invoke(t) ?:
                dialog.context.toast("Could not find an Ethereum address")
    }
}
