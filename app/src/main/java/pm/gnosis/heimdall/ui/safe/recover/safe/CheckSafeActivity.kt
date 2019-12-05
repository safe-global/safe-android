package pm.gnosis.heimdall.ui.safe.recover.safe


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_check_safe.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.AddressInputHelper
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.safe.recover.safe.CheckSafeContract.CheckResult.*
import pm.gnosis.heimdall.ui.tokens.payment.PaymentTokensActivity
import pm.gnosis.heimdall.utils.setCompoundDrawableResource
import pm.gnosis.heimdall.utils.setCompoundDrawables
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class CheckSafeActivity : ViewModelActivity<CheckSafeContract>() {

    @Inject
    lateinit var addressHelper: AddressHelper

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    private var nextIntent: Intent? = null

    private var currentAddress: Solidity.Address? = null

    private lateinit var addressInputHelper: AddressInputHelper

    override fun screenId() = ScreenId.INPUT_SAFE_ADDRESS

    override fun layout() = R.layout.layout_check_safe

    override fun inject(component: ViewComponent) = component.inject(this)

    @Suppress("MoveLambdaOutsideParentheses")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        addressInputHelper.handleResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addressInputHelper = AddressInputHelper(this, ::updateAddress, {
            snackbar(layout_check_safe_address, R.string.invalid_ethereum_address)
        })
        layout_check_safe_next.setCompoundDrawableResource(right = R.drawable.ic_arrow_forward_24dp)
    }

    override fun onStart() {
        super.onStart()
        layout_check_safe_next.isEnabled = false

        disposables += toolbarHelper.setupShadow(layout_check_safe_toolbar_shadow, layout_check_safe_intro_content_scroll)

        disposables += layout_check_safe_input_background.clicks()
            .subscribeBy {
                addressInputHelper.showDialog()
            }

        disposables += layout_check_safe_next.clicks()
            .subscribeBy {
                nextIntent?.let {
                    startActivity(PaymentTokensActivity.createIntent(context = this, safeAddress = currentAddress, hint = getString(R.string.choose_how_to_pay_recovery_fee), nextAction = it))
                }
            }

        disposables += layout_check_safe_back_arrow.clicks().subscribeBy { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        currentAddress?.let {
            layout_check_safe_next.isEnabled = true
        }
    }

    private fun updateAddress(address: Solidity.Address?) {
        currentAddress = address
        if (address != null) {
            layout_check_safe_name.visible(false)
            layout_check_safe_input_background.text = null
            layout_check_safe_address_info.text = null
            layout_check_safe_next.isEnabled = false
            addressHelper.populateAddressInfo(layout_check_safe_address, layout_check_safe_name, layout_check_safe_image, address).forEach {
                disposables += it
            }

            disposables += viewModel.checkSafe(address.asEthereumAddressString())
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(
                    onNext = ::handleCheckResult,
                    onError = ::handleCheckError
                )
        } else {
            layout_check_safe_input_background.setCompoundDrawables(right = ContextCompat.getDrawable(this, R.drawable.ic_more_vert))
            layout_check_safe_input_background.text = getString(R.string.address_hint)
            layout_check_safe_name.visible(false)
            layout_check_safe_name.text = null
            layout_check_safe_address.text = null
            layout_check_safe_image.setAddress(null)
        }
    }

    private fun handleCheckResult(result: CheckSafeContract.CheckResult) {
        when (result) {
            INVALID_SAFE -> {
                setCheckIcon(R.drawable.ic_error)
                layout_check_safe_address_info.text = getString(R.string.invalid_safe_address)
                nextIntent = null
                layout_check_safe_next.isEnabled = false
            }
            VALID_SAFE_WITHOUT_EXTENSION -> {
                setCheckIcon(R.drawable.ic_green_check)
                layout_check_safe_address_info.text = null
                nextIntent = currentAddress?.let { RecoverSafeRecoveryPhraseActivity.createIntent(this, it, null) }
                layout_check_safe_next.isEnabled = true
            }
            VALID_SAFE_WITH_EXTENSION -> {
                setCheckIcon(R.drawable.ic_green_check)
                layout_check_safe_address_info.text = null
                nextIntent = currentAddress?.let { RecoverSafe2FAActivity.createIntent(this, it) }
                layout_check_safe_next.isEnabled = true
            }
        }
    }

    private fun setCheckIcon(iconRes: Int) {
        layout_check_safe_input_background.setCompoundDrawables(right = ContextCompat.getDrawable(this, iconRes))
    }

    private fun handleCheckError(throwable: Throwable) {
        handleCheckResult(INVALID_SAFE)
        (throwable as? LocalizedException)?.let {
            layout_check_safe_address_info.text = it.localizedMessage()
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CheckSafeActivity::class.java)
    }
}
