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
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber

class ReviewPayloadActivity : ViewModelActivity<ReviewPayloadContract>() {
    override fun layout() = R.layout.layout_review_message

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.INCOMING_TRANSACTION_REVIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_review_message_payload.movementMethod = ScrollingMovementMethod()
    }

    override fun onStart() {
        super.onStart()

        val payload = intent.getStringExtra(PAYLOAD_EXTRA) ?: run { finish(); return }
        val safe = intent.getStringExtra(SAFE_EXTRA)?.asEthereumAddress() ?: run { finish(); return }

        viewModel.setup(payload = payload, safe = safe)

        disposables += viewModel.observe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onViewUpdate, onError = Timber::e)

        disposables += layout_review_message_back_button.clicks()
            .subscribeBy(onNext = { finish() }, onError = Timber::e)

        disposables += layout_review_message_confirm.clicks()
            .map { ReviewPayloadContract.UIEvent.ConfirmPayloadClick }
            .subscribeBy(onNext = { viewModel.uiEvents.onNext(it) }, onError = Timber::e)
    }

    private fun onViewUpdate(viewUpdate: ReviewPayloadContract.ViewUpdate) {
        layout_review_message_payload.text = viewUpdate.payload
        layout_review_message_confirm.isEnabled = !viewUpdate.isLoading
    }

    companion object {
        private const val PAYLOAD_EXTRA = "extra.string.payload"
        private const val SAFE_EXTRA = "extra.string.safe"

        fun createIntent(context: Context, payload: String, safe: Solidity.Address) =
            Intent(context, ConfirmMessageActivity::class.java).apply {
                putExtra(PAYLOAD_EXTRA, payload)
                putExtra(SAFE_EXTRA, safe.asEthereumAddressString())
            }
    }
}
