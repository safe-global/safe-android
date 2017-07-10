package pm.gnosis.android.app.wallet.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import com.squareup.moshi.Moshi
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_main.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.GethRepository
import pm.gnosis.android.app.wallet.data.model.Balance
import pm.gnosis.android.app.wallet.data.model.TransactionJson
import pm.gnosis.android.app.wallet.data.remote.EtherscanRepository
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.util.snackbar
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator.QR_CODE_TYPES
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator.SCAN_RESULT_EXTRA
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject lateinit var gethRepo: GethRepository
    @Inject lateinit var moshi: Moshi
    @Inject lateinit var etherscanRepository: EtherscanRepository

    val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_main)
        scan_qr_btn.setOnClickListener {
            val integrator = ZxingIntentIntegrator(this)
            integrator.initiateScan(QR_CODE_TYPES)
        }
        account_address.text = gethRepo.getAccount().address.hex
        Timber.d(account_address.text.toString())
    }

    override fun onStart() {
        super.onStart()
        disposables +=
                etherscanRepository.getEtherBalance()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onNext = this::onBalance, onError = Timber::e)
    }

    private fun onBalance(balance: Balance) {
        account_balance.text = balance.result.toEther().stripTrailingZeros().toPlainString() + " Ξ"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(SCAN_RESULT_EXTRA)) {
                processQrCode(data.getStringExtra(SCAN_RESULT_EXTRA))
            } else if (resultCode == Activity.RESULT_CANCELED) {
                snackbar(coordinator_layout, "Cancelled by the user")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    //TODO: new thread
    fun processQrCode(data: String) {
        try {
            val bytes = Base64.decode(data, Base64.DEFAULT)
            val jsonAdapter = moshi.adapter<TransactionJson>(TransactionJson::class.java)
            val transactionJson = jsonAdapter.fromJson(String(bytes))
            val intent = Intent(this, TransactionDetailsActivity::class.java)
            intent.putExtra(TransactionDetailsActivity.TRANSACTION_EXTRA, transactionJson)
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e)
            snackbar(coordinator_layout, "QRCode does not contain a valid transaction")
        }
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
