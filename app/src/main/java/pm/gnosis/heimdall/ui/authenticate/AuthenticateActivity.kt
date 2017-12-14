package pm.gnosis.heimdall.ui.authenticate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_authenticate.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.scanQrCode
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import timber.log.Timber
import javax.inject.Inject

class AuthenticateActivity : BaseActivity() {

    override fun screenId() = ScreenId.AUTHENTICATE

    private var scannedResults: AuthenticateContract.ActivityResults? = null

    @Inject
    lateinit var viewModel: AuthenticateContract

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_authenticate)
        registerToolbar(layout_authenticate_toolbar)
    }

    override fun onStart() {
        super.onStart()

        layout_authenticate_scan.setOnClickListener {
            scanQrCode()
        }

        scannedResults?.let {
            disposables += viewModel.checkResult(it)
                    .subscribeForResult(::startActivity, ::handleError)
            scannedResults = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        scannedResults = AuthenticateContract.ActivityResults(requestCode, resultCode, data)
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
