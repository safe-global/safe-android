package pm.gnosis.heimdall.ui.messagesigning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import kotlinx.android.synthetic.main.screen_signature_request_details.signature_request_details_back_btn as backBtn
import kotlinx.android.synthetic.main.screen_signature_request_details.signature_request_details_domain_label as domainLabel
import kotlinx.android.synthetic.main.screen_signature_request_details.signature_request_details_message_label as messageLabel

class SignatureRequestDetailsActivity : BaseActivity() {

    override fun screenId() = ScreenId.SIGNATURE_REQUEST_MESSAGE


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_signature_request_details)

        backBtn.setOnClickListener {
            finish()
        }

        intent.extras?.let {

            domainLabel.text = it[EXTRA_DOMAIN] as String
            messageLabel.text = it[EXTRA_MESSAGE] as String
        } ?: finish()
    }


    companion object {

        private const val EXTRA_DOMAIN = "extra_domain"
        private const val EXTRA_MESSAGE = "extra_message"

        fun createIntent(context: Context, domain: String, message: String) =
            Intent(context, SignatureRequestDetailsActivity::class.java).apply {
                putExtra(EXTRA_DOMAIN, domain)
                putExtra(EXTRA_MESSAGE, message)
            }
    }
}