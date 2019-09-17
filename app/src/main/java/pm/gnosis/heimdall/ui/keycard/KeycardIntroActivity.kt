package pm.gnosis.heimdall.ui.keycard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.layout_keycard_intro.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity

class KeycardIntroActivity : BaseActivity() {

    override fun screenId() = ScreenId.KEYCARD_INTRO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_keycard_intro)

        keycard_intro_back_button.setOnClickListener { onBackPressed() }
        keycard_intro_setup.setOnClickListener {  }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, KeycardIntroActivity::class.java)
    }

}