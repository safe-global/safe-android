package pm.gnosis.android.app.wallet.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import org.ethereum.geth.KeyStore
import pm.gnosis.android.app.wallet.GnosisApplication
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject lateinit var keyStore: KeyStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.activity_main)
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(GnosisApplication[this].component)
                .viewModule(ViewModule(this))
                .build().inject(this)
    }
}
