package pm.gnosis.android.app.wallet.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.GethRepository
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.util.toast
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator.QR_CODE_TYPES
import pm.gnosis.android.app.wallet.util.zxing.ZxingIntentIntegrator.SCAN_RESULT_EXTRA
import javax.inject.Inject


class MainActivity : AppCompatActivity() {
    @Inject lateinit var gethRepo: GethRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_main)
        scan_qr_btn.setOnClickListener {
            val integrator = ZxingIntentIntegrator(this)
            integrator.initiateScan(QR_CODE_TYPES)
        }
        account_address.text = gethRepo.getAccount().address.hex
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(SCAN_RESULT_EXTRA)) {
                toast(data.getStringExtra(SCAN_RESULT_EXTRA))
            } else {
                toast("Something went wrong :(")
            }
        }
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
