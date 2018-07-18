package pm.gnosis.heimdall.ui.safe.recover.address


import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_check_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.safe.recover.extension.RecoverPairingActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.parseEthereumAddress
import pm.gnosis.heimdall.utils.setCompoundDrawables
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CheckSafeActivity : ViewModelActivity<CheckSafeContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.INPUT_SAFE_ADDRESS

    override fun layout() = R.layout.layout_check_safe

    override fun inject(component: ViewComponent) = component.inject(this)

    @Suppress("MoveLambdaOutsideParentheses")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data,
            {
                parseEthereumAddress(it)?.let {
                    layout_check_safe_address_input.setText(it.asEthereumAddressString())
                } ?: run {
                    snackbar(layout_check_safe_address_input, R.string.invalid_ethereum_address)
                }
            })
    }

    override fun onStart() {
        super.onStart()

        disposables += toolbarHelper.setupShadow(layout_check_safe_toolbar_shadow, layout_check_safe_intro_content_scroll)

        disposables += layout_check_safe_address_input.textChanges()
            .doOnNext {
                // Set all to null
                layout_check_safe_address_input.setCompoundDrawables()
                layout_check_safe_address_input_info.text = null
                layout_check_safe_next.isEnabled = false
            }
            .debounce(500, TimeUnit.MILLISECONDS)
            .switchMapSingle(viewModel::checkSafe)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(
                onNext = ::handleCheckResult,
                onError = ::handleCheckError
            )

        disposables += layout_check_safe_next.clicks()
            .subscribeBy {
                layout_check_safe_address_input.text.toString().asEthereumAddress()?.let {
                    startActivity(RecoverPairingActivity.createIntent(this, it))
                }
            }

        disposables += layout_check_safe_qr_code_button.clicks()
            .subscribeBy { QRCodeScanActivity.startForResult(this) }

        disposables += layout_check_safe_back_arrow.clicks().subscribeBy { onBackPressed() }
    }

    private fun handleCheckResult(result: Boolean) {
        layout_check_safe_address_input.setCompoundDrawables(
            right = ContextCompat.getDrawable(this, if (result) R.drawable.ic_green_check else R.drawable.ic_error)
        )
        layout_check_safe_address_input_info.text = if (result) null else getString(R.string.invalid_safe_address)
        layout_check_safe_next.isEnabled = result
    }

    private fun handleCheckError(throwable: Throwable) {
        handleCheckResult(false)
        (throwable as? LocalizedException)?.let {
            layout_check_safe_address_input_info.text = it.localizedMessage()
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CheckSafeActivity::class.java)
    }
}
