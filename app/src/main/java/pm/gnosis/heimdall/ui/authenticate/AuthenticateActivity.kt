package pm.gnosis.heimdall.ui.authenticate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_authenticate.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.svalinn.common.utils.scanQrCode
import pm.gnosis.svalinn.common.utils.subscribeForResult
import timber.log.Timber
import javax.inject.Inject

class AuthenticateActivity : BaseActivity() {

    override fun screenId() = ScreenId.AUTHENTICATE

    @Inject
    lateinit var viewModel: AuthenticateContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_authenticate)
        setupToolbar(layout_authenticate_toolbar, R.drawable.ic_close_24dp)
    }

    override fun onStart() {
        super.onStart()

        layout_authenticate_transaction_input_button.setOnClickListener {
            scanQrCode()
        }

        disposables += layout_authenticate_review_button.clicks()
            .switchMap {
                viewModel.checkResult(layout_authenticate_transaction_input.text.toString())
            }
            .subscribeForResult(::startActivity, ::handleError)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleQrCodeActivityResult(requestCode, resultCode, data, {
            layout_authenticate_transaction_input.setText(it)
        })
    }

    private fun handleError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_scan_coordinator, throwable)
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build().inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, AuthenticateActivity::class.java)
    }
}
