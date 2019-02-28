package pm.gnosis.heimdall.ui.messagesigning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_collect_message_signatures.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.toHexString
import timber.log.Timber

class CollectMessageSignaturesActivity : ViewModelActivity<CollectMessageSignaturesContract>() {
    override fun layout(): Int = R.layout.layout_collect_message_signatures

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.INCOMING_TRANSACTION_REVIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val payload = intent.getStringExtra(PAYLOAD_EXTRA) ?: run { finish(); return }
        val safe = intent.getStringExtra(SAFE_ADDRESS_EXTRA).asEthereumAddress() ?: run { finish(); return }
        val threshold = intent.getLongExtra(THRESHOLD_EXTRA, -1).let {
            if (it == -1L) {
                finish(); return
            } else it
        }
        val owners = nullOnThrow { intent.getStringArrayExtra(OWNERS_EXTRA).map { it.asEthereumAddress()!! } } ?: run { finish(); return }
        val deviceSignature = intent.getStringExtra(DEVICE_SIGNATURE_EXTRA)?.let { Signature.from(it) } ?: run { finish(); return }
        viewModel.setup(payload = payload, safe = safe, threshold = threshold, owners = owners.toList(), deviceSignature = deviceSignature)
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onViewUpdate, onError = Timber::e)
    }

    private fun onViewUpdate(viewUpdate: CollectMessageSignaturesContract.ViewUpdate) {
        layout_collect_message_signatures_request_refresh.visible(viewUpdate.inProgress, hiddenVisibility = View.INVISIBLE)
        layout_collect_message_signatures_request_button.isEnabled = !viewUpdate.inProgress

        viewUpdate.signature?.let { signature ->
            layout_collect_message_signatures_signature.text = signature.toHexString()
            layout_collect_message_signatures_signature.visible(true)
            layout_collect_message_signatures_share.visible(true)
            layout_collect_message_signatures_request_button.isEnabled = false
        }

        layout_collect_message_signatures_received.text = viewUpdate.signaturesReceived.toString()
    }

    companion object {
        private const val PAYLOAD_EXTRA = "extra.string.payload"
        private const val SAFE_ADDRESS_EXTRA = "extra.string.safe"
        private const val THRESHOLD_EXTRA = "extra.long.threshold"
        private const val OWNERS_EXTRA = "extra.string.owners"
        private const val DEVICE_SIGNATURE_EXTRA = "extra.string.owners"

        fun createIntent(
            context: Context,
            payload: String,
            safe: Solidity.Address,
            threshold: Long,
            owners: List<Solidity.Address>,
            deviceSignature: Signature
        ) = Intent(context, CollectMessageSignaturesActivity::class.java).apply {
            putExtra(PAYLOAD_EXTRA, payload)
            putExtra(SAFE_ADDRESS_EXTRA, safe.asEthereumAddressString())
            putExtra(THRESHOLD_EXTRA, threshold)
            putStringArrayListExtra(OWNERS_EXTRA, owners.map { it.asEthereumAddressString() }.toCollection(ArrayList()))
            putExtra(DEVICE_SIGNATURE_EXTRA, deviceSignature.toString())
        }
    }
}
