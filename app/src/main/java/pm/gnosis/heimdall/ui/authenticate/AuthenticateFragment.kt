package pm.gnosis.heimdall.ui.authenticate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_authenticate.*
import kotlinx.android.synthetic.main.layout_security.*
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.scanQrCode
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.transactiondetails.TransactionDetailsActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.hexToByteArray
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class AuthenticateFragment : BaseFragment() {

    private val resultSubject = PublishSubject.create<AuthenticateContract.ActivityResults>()

    @Inject
    lateinit var viewModel: AuthenticateContract

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.layout_authenticate, container, false)

    override fun onStart() {
        super.onStart()
        disposables += resultSubject
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(viewModel::checkResult)
                .subscribeForResult(this::startActivity, this::handleError)
        layout_authenticate_scan.setOnClickListener {
            //scanQrCode()
            val txData = GnosisSafe.ConfirmTransaction.encode(Solidity.Bytes32("c9a99d7a39348b5a2acca1bfc15ec079d23135003946a099f9100b0614f6b10d".hexToByteArray()))
            val descriptionHash = "c8f7181d7db0c195ed18b20afbbb9240801de860c93b033aaade645396c38b5b"
            startActivity(TransactionDetailsActivity.createIntent(activity!!, Transaction(BigInteger.ONE, data = txData), descriptionHash))
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

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(context!!))
                .build().inject(this)
    }
}
