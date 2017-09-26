package pm.gnosis.heimdall.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_onboarding.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.base.BaseActivity

class OnBoardingActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_onboarding)

        layout_onboarding_create.setOnClickListener {
            startActivity(GenerateMnemonicActivity.createIntent(this))
        }

        layout_onboarding_restore.setOnClickListener {
            startActivity(RestoreAccountActivity.createIntent(this))
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, OnBoardingActivity::class.java)
    }
}
