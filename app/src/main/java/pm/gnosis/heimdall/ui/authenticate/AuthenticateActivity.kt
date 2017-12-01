package pm.gnosis.heimdall.ui.authenticate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_authenticate.*
import kotlinx.android.synthetic.main.layout_security.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.scanQrCode
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import timber.log.Timber
import javax.inject.Inject

class AuthenticateActivity : BaseActivity() {

    private val resultSubject = PublishSubject.create<AuthenticateContract.ActivityResults>()

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
        disposables += resultSubject
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(viewModel::checkResult)
                .subscribeForResult(::startActivity, ::handleError)
        layout_authenticate_scan.setOnClickListener {
            scanQrCode()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resultSubject.onNext(AuthenticateContract.ActivityResults(requestCode, resultCode, data))
    }

    private fun handleError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_security_content_container, throwable)
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
