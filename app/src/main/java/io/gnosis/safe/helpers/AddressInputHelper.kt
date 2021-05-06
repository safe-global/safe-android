package io.gnosis.safe.helpers

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.Tracker
import io.gnosis.safe.databinding.BottomSheetAddressInputBinding
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.ui.base.fragment.BaseFragment
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.ui.dialogs.UnstoppableInputDialog
import io.gnosis.safe.utils.handleAddressBookResult
import io.gnosis.safe.utils.handleQrCodeActivityResult
import io.gnosis.safe.utils.parseEthereumAddress
import pm.gnosis.model.Solidity
import pm.gnosis.utils.exceptions.InvalidAddressException
import timber.log.Timber

class AddressInputHelper(
    fragment: BaseFragment,
    tracker: Tracker,
    private val addressCallback: (Solidity.Address) -> Unit,
    private val errorCallback: (Throwable, String?) -> Unit
) {

    private val dialog =
        BottomSheetDialog(fragment.requireContext(), R.style.DayNightBottomSheetDialogTheme).apply {
            val clipboard = fragment.activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val binding = BottomSheetAddressInputBinding.inflate(layoutInflater)
            with(binding) {
                setContentView(root)

                bottomSheetAddressInputUnstoppabledomains.setOnClickListener {
                    UnstoppableInputDialog.create().apply {
                        callback = addressCallback
                        show(fragment.childFragmentManager, null)
                    }
                    dismiss()
                }

                bottomSheetAddressInputEnsTouch.setOnClickListener {
                    EnsInputDialog.create().apply {
                        callback = addressCallback
                        show(fragment.childFragmentManager, null)
                    }
                    dismiss()
                }
                bottomSheetAddressInputQrTouch.setOnClickListener {
                    QRCodeScanActivity.startForResult(fragment)
                    tracker.logScreen(ScreenId.SCANNER)
                    dismiss()
                }
                bottomSheetAddressInputPasteTouch.setOnClickListener {
                    val input = clipboard.primaryClip?.getItemAt(0)?.text?.trim()
                    (input?.let { parseEthereumAddress(it.toString()) }
                        ?: run {
                            fragment.context?.let {
                                handleError(InvalidAddressException(fragment.getString(R.string.invalid_ethereum_address)), input.toString())
                            } ?: Timber.e(InvalidAddressException(), "Fragment lost context.")
                            null
                        })?.let { addressCallback(it) }
                    dismiss()
                }
            }
        }

    fun showDialog() = dialog.show()

    fun hideDialog() = dialog.hide()

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("MoveLambdaOutsideParentheses")
        if (!handleQrCodeActivityResult(requestCode, resultCode, data, {
                addressCallback(parseEthereumAddress(it) ?: run {
                    handleError(InvalidAddressException(it), it)
                    return@handleQrCodeActivityResult
                })
            })) {
            @Suppress("MoveLambdaOutsideParentheses")
            handleAddressBookResult(requestCode, resultCode, data, {
                addressCallback(it.address)
            })
        }
    }

    private fun handleError(error: Throwable, input: String) {
        errorCallback.invoke(error, input)
    }
}
