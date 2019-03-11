package pm.gnosis.heimdall.ui.messagesigning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_review_message.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.toast
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber

class ConfirmMessageActivity : ViewModelActivity<ConfirmMessageContract>() {

    override fun screenId() = ScreenId.INCOMING_TRANSACTION_REVIEW

    override fun layout() = R.layout.layout_review_message

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_review_message_payload.movementMethod = ScrollingMovementMethod()
    }

    override fun onStart() {
        super.onStart()

        val payload = intent.getStringExtra(PAYLOAD_EXTRA) ?: run { finish(); return }
        val safe = intent.getStringExtra(SAFE_EXTRA)?.asEthereumAddress() ?: run { finish(); return }
        val extensionSignature = intent.getStringExtra(EXTENSION_SIGNATURE_EXTRA)?.let { Signature.from(it) } ?: run { finish(); return }

        viewModel.setup(payload, safe, extensionSignature)

        disposables += layout_review_message_back_button.clicks()
            .subscribeBy(onNext = { finish() }, onError = Timber::e)

        disposables += layout_review_message_confirm.clicks()
            .map { ConfirmMessageContract.UIEvent.ConfirmPayloadClick }
            .subscribeBy(onNext = { viewModel.uiEvents.onNext(it) }, onError = Timber::e)

        disposables += viewModel.observe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onViewUpdate, onError = Timber::e)
    }

    private fun onViewUpdate(viewUpdate: ConfirmMessageContract.ViewUpdate) {
        layout_review_message_payload.text = viewUpdate.payload

        viewUpdate.error?.let { error ->
            snackbar(
                layout_review_message_root, when (error) {
                    is ConfirmMessageContract.InvalidPayload -> R.string.invalid_eip712_message
                    is ConfirmMessageContract.ErrorRecoveringSender -> R.string.error_recovering_message_sender
                    is ConfirmMessageContract.ErrorSigningHash -> R.string.error_signing_message
                    is ConfirmMessageContract.ErrorSendingPush -> R.string.message_requester_not_paired
                    else -> R.string.unknown_error
                }
            )
        }

        if (viewUpdate.finishProcess) {
            toast(R.string.confirmation_sent)
            finish()
        }
        layout_review_message_confirm.isEnabled = !viewUpdate.isLoading
    }

    override fun inject(component: ViewComponent) =
        component.inject(this)

    companion object {
        private const val PAYLOAD_EXTRA = "extra.string.payload"
        private const val SAFE_EXTRA = "extra.string.safe"
        private const val EXTENSION_SIGNATURE_EXTRA = "extra.string.extension_signature"

        fun createIntent(context: Context, payload: String, safe: Solidity.Address, extensionSignature: Signature) =
            Intent(context, ConfirmMessageActivity::class.java).apply {
                putExtra(PAYLOAD_EXTRA, payload)
                putExtra(SAFE_EXTRA, safe.asEthereumAddressString())
                putExtra(EXTENSION_SIGNATURE_EXTRA, extensionSignature.toString())
            }
    }
}
