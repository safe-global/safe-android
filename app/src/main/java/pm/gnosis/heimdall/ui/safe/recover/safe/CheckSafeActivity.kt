package pm.gnosis.heimdall.ui.safe.recover.safe


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
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
import pm.gnosis.heimdall.ui.safe.recover.safe.CheckSafeContract.CheckResult.*
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.parseEthereumAddress
import pm.gnosis.heimdall.utils.setCompoundDrawableResource
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

    private var nextIntent: Intent? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_check_safe_next.setCompoundDrawableResource(right = R.drawable.ic_arrow_forward_24dp)
    }

    override fun onStart() {
        super.onStart()
        layout_check_safe_next.isEnabled = false

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
                nextIntent?.let { startActivity(it) }
            }

        disposables += layout_check_safe_qr_code_button.clicks()
            .subscribeBy { QRCodeScanActivity.startForResult(this) }

        disposables += layout_check_safe_back_arrow.clicks().subscribeBy { onBackPressed() }
    }

    private fun handleCheckResult(result: CheckSafeContract.CheckResult) {
        when (result) {
            INVALID_SAFE -> {
                setCheckIcon(R.drawable.ic_error)
                layout_check_safe_address_input_info.text = getString(R.string.invalid_safe_address)
                nextIntent = null
                layout_check_safe_next.isEnabled = false
            }
            VALID_SAFE_WITHOUT_EXTENSION -> {
                setCheckIcon(R.drawable.ic_green_check)
                layout_check_safe_address_input_info.text = null
                nextIntent = layout_check_safe_address_input.text.toString().asEthereumAddress()?.let {
                    RecoverSafeRecoveryPhraseActivity.createIntent(this, it, null)
                }
                layout_check_safe_next.isEnabled = true
            }
            VALID_SAFE_WITH_EXTENSION -> {
                setCheckIcon(R.drawable.ic_green_check)
                layout_check_safe_address_input_info.text = null
                nextIntent =
                        layout_check_safe_address_input.text.toString().asEthereumAddress()?.let { RecoverSafePairingActivity.createIntent(this, it) }
                layout_check_safe_next.isEnabled = true
            }
        }
    }

    private fun setCheckIcon(iconRes: Int) {
        layout_check_safe_address_input.setCompoundDrawables(right = ContextCompat.getDrawable(this, iconRes))
    }

    private fun handleCheckError(throwable: Throwable) {
        handleCheckResult(INVALID_SAFE)
        (throwable as? LocalizedException)?.let {
            layout_check_safe_address_input_info.text = it.localizedMessage()
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CheckSafeActivity::class.java)
    }
}
