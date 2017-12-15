package pm.gnosis.heimdall.ui.onboarding.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_account_setup.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.onboarding.account.create.GenerateMnemonicActivity
import pm.gnosis.heimdall.ui.onboarding.account.restore.RestoreAccountActivity

class AccountSetupActivity : BaseActivity() {
    override fun screenId() = ScreenId.ACCOUNT_SETUP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_account_setup)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_account_setup_create.clicks()
                .subscribeBy(onNext = {
                    startActivity(GenerateMnemonicActivity.createIntent(this))
                })

        disposables += layout_account_setup_restore.clicks()
                .subscribeBy(onNext = {
                    startActivity(RestoreAccountActivity.createIntent(this))
                })
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, AccountSetupActivity::class.java)
    }
}
