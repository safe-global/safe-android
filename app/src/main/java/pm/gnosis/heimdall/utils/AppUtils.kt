package pm.gnosis.heimdall.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.ui.addressbook.list.AddressBookActivity
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.utils.hexAsEthereumAddressOrNull

fun errorSnackbar(view: View, throwable: Throwable, duration: Int = Snackbar.LENGTH_LONG) {
    val message = (throwable as? LocalizedException)?.localizedMessage()
            ?: view.context.getString(R.string.error_try_again)
    snackbar(view, message, duration)
}

fun Context.errorToast(throwable: Throwable, duration: Int = Toast.LENGTH_LONG) {
    val message = (throwable as? LocalizedException)?.localizedMessage()
            ?: getString(R.string.error_try_again)
    toast(message, duration)
}

fun handleQrCodeActivityResult(requestCode: Int, resultCode: Int, data: Intent?,
                               onQrCodeResult: (String) -> Unit, onCancelledResult: (() -> Unit)? = null): Boolean {
    if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)) {
            onQrCodeResult(data.getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA))
        } else if (resultCode == Activity.RESULT_CANCELED) {
            onCancelledResult?.invoke()
        }
        return true
    }
    return false
}

fun Activity.selectFromAddressBook() = startActivityForResult(AddressBookActivity.createIntent(this), AddressBookActivity.REQUEST_CODE)
fun Fragment.selectFromAddressBook() = startActivityForResult(AddressBookActivity.createIntent(context!!), AddressBookActivity.REQUEST_CODE)

fun handleAddressBookResult(requestCode: Int, resultCode: Int, data: Intent?,
                            onResult: (AddressBookEntry) -> Unit, onCancelled: (() -> Unit)? = null): Boolean {
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

fun parseEthereumAddress(address: String) =
        address.hexAsEthereumAddressOrNull() ?: ERC67Parser.parse(address)?.transaction?.address

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
    this.text = SpannableStringBuilder(text)
            .append(" ")
            .appendText(context.getString(R.string.etherscan_io), URLSpan(url))
            .append(" ")
            .appendText(" ", ImageSpan(linkDrawable, ImageSpan.ALIGN_BASELINE))
    setOnClickListener { this.context.openUrl(url) }
}
