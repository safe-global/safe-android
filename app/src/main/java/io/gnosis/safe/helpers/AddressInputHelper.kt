package io.gnosis.safe.helpers

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.ScreenId
import io.gnosis.safe.Tracker
import io.gnosis.safe.databinding.BottomSheetAddressInputBinding
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import io.gnosis.safe.ui.base.fragment.BaseFragment
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.utils.handleAddressBookResult
import io.gnosis.safe.utils.handleQrCodeActivityResult
import io.gnosis.safe.utils.parseEthereumAddress
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.exceptions.InvalidAddressException

class AddressInputHelper(
    fragment: BaseFragment,
    tracker: Tracker,
    private val addressCallback: (Solidity.Address) -> Unit,
    private val errorCallback: ((Throwable) -> Unit),
    allowAddressBook: Boolean = false
) {

    private val dialog =
        BottomSheetDialog(fragment.requireContext()).apply {
            val clipboard = fragment.activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val binding = BottomSheetAddressInputBinding.inflate(layoutInflater)
            with(binding) {
                setContentView(root)

                bottomSheetAddressInputBook.visible(allowAddressBook)
                bottomSheetAddressInputBookIcon.visible(allowAddressBook)
                bottomSheetAddressInputBookTouch.visible(allowAddressBook)

                if (allowAddressBook) {
                    bottomSheetAddressInputBookTouch.setOnClickListener {
                        // TODO uncomment when address book functionality is ready
//                    activity.selectFromAddressBook()
                        hide()
                    }
                }
                bottomSheetAddressInputEnsTouch.setOnClickListener {
                    EnsInputDialog.create().apply {
                        callback = addressCallback
                        show(fragment.childFragmentManager, null)
                    }
                    hide()
                }
                bottomSheetAddressInputQrTouch.setOnClickListener {
                    QRCodeScanActivity.startForResult(fragment)
                    tracker.logScreen(ScreenId.SCANNER)
                    hide()
                }
                bottomSheetAddressInputPasteTouch.setOnClickListener {
                    (clipboard.primaryClip?.getItemAt(0)?.text?.trim()?.let { parseEthereumAddress(it.toString()) }
                        ?: run {
                            handleError(InvalidAddressException("No Ethereum address found"))
                            null
                        })?.let { addressCallback(it) }
                    hide()
                }
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
        errorCallback.invoke(t)
    }
}
