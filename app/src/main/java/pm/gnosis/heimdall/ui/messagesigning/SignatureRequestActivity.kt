package pm.gnosis.heimdall.ui.messagesigning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.asMiddleEllipsized
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_barrier_bottom_panel as bottomPanel
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_dapp_contract_address as dappAddressLabel
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_dapp_contract_img as dappImage
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_dapp_name as dappNameLabel
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_root as root
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_safe_address as safeAddressLabel
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_safe_balance as safeBalanceLabel
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_safe_image as safeImage
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_safe_name as safeNameLabel
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_show_message as showMessageBtn
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_status_panel as statusPanel
import kotlinx.android.synthetic.main.screen_signature_request.signature_request_toolbar_back_btn as backBtn

class SignatureRequestActivity : ViewModelActivity<SignatureRequestContract>() {

    override fun screenId() = ScreenId.SIGNATURE_REQUEST_REVIEW

    override fun layout() = R.layout.screen_signature_request

    override fun inject(component: ViewComponent) =
        component.inject(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val payload = intent.getStringExtra(PAYLOAD_EXTRA) ?: run { finish(); return }
        val safe = intent.getStringExtra(SAFE_EXTRA)?.asEthereumAddress() ?: run { finish(); return }
        val extensionSignature = intent.getStringExtra(EXTENSION_SIGNATURE_EXTRA)?.let { Signature.from(it) }

        viewModel.setup(payload, safe, extensionSignature)
        viewModel.state.observe(this, Observer {
            onViewUpdate(it)
        })

        backBtn.setOnClickListener {
            finish()
        }

        showMessageBtn.setOnClickListener {
            startActivity(SignatureRequestDetailsActivity.createIntent(this, viewModel.viewData.domainPayload, viewModel.viewData.messagePayload))
        }

        bottomPanel.disabled = false
        bottomPanel.forwardClickAction = {
            viewModel.confirmPayload()
        }
    }

    private fun onViewUpdate(viewUpdate: SignatureRequestContract.ViewUpdate) {

        bottomPanel.disabled = viewUpdate.isLoading

        viewUpdate.error?.let { error ->
            snackbar(
                root, when (error) {
                    is SignatureRequestContract.InvalidPayload -> R.string.invalid_eip712_message
                    is SignatureRequestContract.ErrorRecoveringSender -> R.string.error_recovering_message_sender
                    is SignatureRequestContract.ErrorSigningHash -> R.string.error_signing_message
                    is SignatureRequestContract.ErrorSendingPush -> R.string.message_requester_not_paired
                    else -> R.string.unknown_error
                }
            )
            Timber.e(error)
        }

        if (viewUpdate.finishProcess) {
            toast(R.string.confirmation_sent)
            finish()
        }

        with(viewUpdate.viewData) {

            safeImage.setAddress(safeAddress)
            safeAddressLabel.text = safeAddress.asEthereumAddressChecksumString().asMiddleEllipsized(4)
            safeNameLabel.text = safeName
            safeBalanceLabel.text = safeBalance

            dappImage.setAddress(dappAddress)
            dappAddressLabel.text = dappAddress.asEthereumAddressChecksumString().asMiddleEllipsized(4)
            dappNameLabel.text = dappName

            when (status) {
                SignatureRequestContract.Status.READY_TO_SIGN -> {
                    statusPanel.visibility = View.GONE
                    bottomPanel.visibility = View.VISIBLE
                }
                SignatureRequestContract.Status.AUTHORIZATION_REQUIRED -> {
                    statusPanel.actionPrimary = false
                    statusPanel.title = getString(R.string.authentication_required)
                    statusPanel.message = getString(R.string.signature_request_explanation_authentication)
                    statusPanel.image = R.drawable.img_awaiting_confirmation
                    statusPanel.actionTitle = getString(R.string.request_confirmation)
                    statusPanel.action = {
                        viewModel.resend()
                    }
                    statusPanel.visibility = View.VISIBLE
                    bottomPanel.visibility = View.GONE
                }
                SignatureRequestContract.Status.AUTHORIZATION_REJECTED -> {
                    statusPanel.actionPrimary = true
                    statusPanel.title = getString(R.string.rejected)
                    statusPanel.message = getString(R.string.signature_request_explanation_rejected)
                    statusPanel.image = R.drawable.img_rejected
                    statusPanel.actionTitle = getString(R.string.resend)
                    statusPanel.action = {
                        viewModel.resend()
                    }
                    statusPanel.visibility = View.VISIBLE
                    bottomPanel.visibility = View.GONE
                }
                SignatureRequestContract.Status.AUTHORIZATION_APPROVED -> {
                    statusPanel.actionPrimary = true
                    statusPanel.title = getString(R.string.confirmed)
                    statusPanel.message = getString(R.string.signature_request_explanation_confirmed)
                    statusPanel.image = R.drawable.img_confirmed
                    statusPanel.actionTitle = getString(R.string.sign)
                    statusPanel.action = {
                        viewModel.sign()
                    }
                    statusPanel.visibility = View.VISIBLE
                    bottomPanel.visibility = View.GONE
                }
            }
        }
    }

    companion object {

        private const val PAYLOAD_EXTRA = "extra.string.payload"
        private const val SAFE_EXTRA = "extra.string.safe"
        private const val EXTENSION_SIGNATURE_EXTRA = "extra.string.extension_signature"

        fun createIntent(context: Context, payload: String, safe: Solidity.Address, extensionSignature: Signature? = null): Intent =
            Intent(context, SignatureRequestActivity::class.java).apply {
                putExtra(PAYLOAD_EXTRA, payload)
                putExtra(SAFE_EXTRA, safe.asEthereumAddressString())
                putExtra(EXTENSION_SIGNATURE_EXTRA, extensionSignature?.toString())
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
    }
}