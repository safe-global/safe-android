package pm.gnosis.heimdall.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.openUrl
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.svalinn.utils.ethereum.ERC67Parser
import pm.gnosis.utils.asEthereumAddress
import java.lang.ref.WeakReference

fun errorSnackbar(
    view: View,
    throwable: Throwable,
    duration: Int = Snackbar.LENGTH_LONG,
    @StringRes defaultErrorMsg: Int = R.string.error_try_again,
    action: Pair<String, (View) -> Unit>? = null
): Snackbar {
    val message = (throwable as? LocalizedException)?.localizedMessage() ?: run {
        @Suppress("ConstantConditionIf")
        if (BuildConfig.VERBOSE_EXCEPTIONS) "${throwable.javaClass.simpleName}: ${throwable.message}"
        else view.context.getString(defaultErrorMsg)
    }

    return snackbar(view, message, duration, action)
}

fun Context.errorToast(throwable: Throwable, duration: Int = Toast.LENGTH_LONG) {
    val message = (throwable as? LocalizedException)?.localizedMessage() ?: getString(R.string.error_try_again)
    toast(message, duration)
}

fun handleQrCodeActivityResult(
    requestCode: Int, resultCode: Int, data: Intent?,
    onQrCodeResult: (String) -> Unit, onCancelledResult: (() -> Unit)? = null
): Boolean {
    if (requestCode == QRCodeScanActivity.REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(QRCodeScanActivity.RESULT_EXTRA)) {
            onQrCodeResult(data.getStringExtra(QRCodeScanActivity.RESULT_EXTRA))
        } else if (resultCode == Activity.RESULT_CANCELED) {
            onCancelledResult?.invoke()
        }
        return true
    }
    return false
}

fun Activity.selectFromAddressBook() = startActivityForResult(AddressBookActivity.createIntent(this), AddressBookActivity.REQUEST_CODE)

fun Fragment.selectFromAddressBook() = startActivityForResult(AddressBookActivity.createIntent(context!!), AddressBookActivity.REQUEST_CODE)

fun handleAddressBookResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    onResult: (AddressBookEntry) -> Unit,
    onCancelled: (() -> Unit)? = null
): Boolean {
    if (requestCode == AddressBookActivity.REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK) {
            AddressBookActivity.parseResult(data)?.let { onResult(it) }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            onCancelled?.invoke()
        }
        return true
    }
    return false
}

fun parseEthereumAddress(address: String) = address.asEthereumAddress() ?: ERC67Parser.parse(address)?.address

fun TextView.setupEtherscanTransactionUrl(transactionHash: String, @StringRes stringId: Int) {
    setupEtherscanTransactionUrl(transactionHash, context.getString(stringId))
}

fun TextView.setupEtherscanTransactionUrl(transactionHash: String, text: String) {
    setupEtherscanLink(context.getString(R.string.etherscan_transaction_url, transactionHash), text)
}

fun TextView.setupEtherscanAddressUrl(address: String, @StringRes stringId: Int) {
    setupEtherscanAddressUrl(address, context.getString(stringId))
}

fun TextView.setupEtherscanAddressUrl(address: String, text: String) {
    setupEtherscanLink(context.getString(R.string.etherscan_address_url, address), text)
}

private fun TextView.setupEtherscanLink(url: String, text: String) {
    val linkDrawable = ContextCompat.getDrawable(this.context, R.drawable.ic_external_link)!!
    linkDrawable.setBounds(0, 0, linkDrawable.intrinsicWidth, linkDrawable.intrinsicHeight)
    this.text = SpannableStringBuilder(Html.fromHtml(text))
        .append(" ")
        .appendText(" ", ImageSpan(linkDrawable, ImageSpan.ALIGN_BASELINE))
    setOnClickListener { this.context.openUrl(url) }
}

fun <T> weak(value: T) = WeakReference(value)
