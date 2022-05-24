package io.gnosis.safe.helpers

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.gnosis.data.models.Chain
import io.gnosis.safe.ui.safe.add.AddressPrefixMismatch
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.Tracker
import io.gnosis.safe.databinding.BottomSheetAddressInputBinding
import io.gnosis.safe.qrscanner.QRCodeScanActivity
import io.gnosis.safe.ui.base.fragment.BaseFragment
import io.gnosis.safe.ui.dialogs.EnsInputDialog
import io.gnosis.safe.ui.dialogs.UnstoppableInputDialog
import io.gnosis.safe.utils.handleAddressBookResult
import io.gnosis.safe.utils.handleQrCodeActivityResult
import io.gnosis.safe.utils.parseEthereumAddress
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.exceptions.InvalidAddressException
import timber.log.Timber

class AddressInputHelper(
    fragment: BaseFragment,
    tracker: Tracker,
    private val selectedChain: Chain,
    private val addressCallback: (Solidity.Address) -> Unit,
    private val errorCallback: (Throwable, String?) -> Unit,
    private val enableUD: Boolean,
    private val enableENS: Boolean

) {

    private val dialog =
        BottomSheetDialog(fragment.requireContext(), R.style.DayNightBottomSheetDialogTheme).apply {
            val clipboard = fragment.activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val binding = BottomSheetAddressInputBinding.inflate(layoutInflater)
            with(binding) {
                setContentView(root)

                if (enableUD) {
                    bottomSheetAddressInputUnstoppabledomains.setOnClickListener {
                        UnstoppableInputDialog.create(selectedChain).apply {
                            callback = addressCallback
                            show(fragment.childFragmentManager, null)
                        }
                        dismiss()
                    }
                } else {
                    bottomSheetAddressInputUnstoppabledomains.visible(false)
                    bottomSheetAddressInputUnstoppabledomainsIcon.visible(false)
                    bottomSheetAddressInputUnstoppabledomainsTouch.visible(false)
                }

                if (enableENS) {
                    bottomSheetAddressInputEnsTouch.setOnClickListener {
                        EnsInputDialog.create(selectedChain).apply {
                            callback = addressCallback
                            show(fragment.childFragmentManager, null)
                        }
                        dismiss()
                    }
                } else {
                    bottomSheetAddressInputEns.visible(false)
                    bottomSheetAddressInputEnsIcon.visible(false)
                    bottomSheetAddressInputEnsTouch.visible(false)
                }

                bottomSheetAddressInputQrTouch.setOnClickListener {
                    QRCodeScanActivity.startForResult(fragment)
                    tracker.logScreen(ScreenId.SCANNER, selectedChain.chainId)
                    dismiss()
                }
                bottomSheetAddressInputPasteTouch.setOnClickListener {

                    val input = clipboard.primaryClip?.getItemAt(0)?.text?.trim().toString()

                    var prefix = ""

                    val address =
                        input?.let {
                            //FIXME: implement proper support for EIP-3770 addresses
                            if (it.contains(":")) {
                                prefix = it.split(":")[0]
                                parseEthereumAddress(it.split(":")[1])
                            } else {
                                parseEthereumAddress(it)
                            }
                        }

                    when {

                        address == null -> {
                            fragment.context?.let {
                                handleError(
                                    InvalidAddressException(fragment.getString(R.string.invalid_ethereum_address)),
                                    input
                                )
                            } ?: Timber.e(InvalidAddressException(), "Fragment lost context.")
                        }

                        prefix.isNotEmpty() && selectedChain.shortName != prefix -> {
                            fragment.context?.let {
                                handleError(
                                    AddressPrefixMismatch,
                                    input
                                )
                            } ?: Timber.e(InvalidAddressException(), "Fragment lost context.")
                        }

                        else -> {
                            addressCallback(address)
                        }
                    }

                    dismiss()
                }
            }
        }

    fun showDialog() = dialog.show()

    fun hideDialog() = dialog.hide()

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("MoveLambdaOutsideParentheses")
        if (!handleQrCodeActivityResult(requestCode, resultCode, data, {
                addressCallback(
                    //FIXME: implement proper support for EIP-3770 addresses
                    parseEthereumAddress(if (it.contains(":")) it.split(":")[1] else it) ?: run {
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
