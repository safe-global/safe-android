package pm.gnosis.heimdall.ui.tokens.receive


import android.content.Context
import android.content.Intent
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.view.longClicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_receive_token.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class ReceiveTokenActivity : ViewModelActivity<ReceiveTokenContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.SAFE_SHARE_ADDRESS

    override fun layout() = R.layout.layout_receive_token

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onStart() {
        super.onStart()
        val safeAddress = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run {
            finish()
            return
        }
        layout_receive_token_safe_image.setAddress(safeAddress)
        layout_receive_token_share_button.isEnabled = false
        disposables += viewModel.observeSafeInfo(safeAddress)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::applyUpdate, onError = Timber::e)

        disposables += toolbarHelper.setupShadow(layout_receive_token_toolbar_shadow, layout_receive_token_content_scroll)

        disposables += layout_receive_token_back_button.clicks().subscribeBy { onBackPressed() }
    }

    private fun applyUpdate(update: ReceiveTokenContract.ViewUpdate) {
        when (update) {
            is ReceiveTokenContract.ViewUpdate.Address -> {

                //make first & last 4 characters black
                val addressString = SpannableStringBuilder(update.checksumAddress)
                addressString.setSpan(ForegroundColorSpan(getColorCompat(R.color.address_boundaries)), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                addressString.setSpan(ForegroundColorSpan(getColorCompat(R.color.address_boundaries)), addressString.length - 4, addressString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                layout_receive_token_safe_address.text = addressString

                disposables += Observable.merge(
                    layout_receive_token_safe_address.longClicks(),
                    layout_receive_token_qr_card.longClicks()
                ).subscribeBy { shareViaClipboard(update.checksumAddress) }
                layout_receive_token_share_button.isEnabled = true
                disposables += layout_receive_token_share_button.clicks()
                    .subscribeBy { shareExternalText(update.checksumAddress, R.string.share_address) }
            }
            is ReceiveTokenContract.ViewUpdate.Info ->
                layout_receive_token_safe_name.text = update.name
            is ReceiveTokenContract.ViewUpdate.QrCode -> {
                layout_receive_token_qr_progress.visible(false)
                layout_receive_token_qr_image.visible(true)
                layout_receive_token_qr_image.setImageBitmap(update.qrCode)
            }
        }
    }

    private fun shareViaClipboard(checksumAddress: String) {
        copyToClipboard(getString(R.string.share_address), checksumAddress) {
            snackbar(layout_receive_token_safe_address, R.string.address_clipboard_success)
        }
    }

    companion object {
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        fun createIntent(context: Context, safeAddress: Solidity.Address) =
            Intent(context, ReceiveTokenActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safeAddress.asEthereumAddressString())
            }
    }
}
