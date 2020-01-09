package pm.gnosis.heimdall.ui.settings.general

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.screen_get_in_touch.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.svalinn.common.utils.toast

class GetInTouchActivity : BaseActivity() {

    override fun screenId() = ScreenId.GET_IN_TOUCH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_get_in_touch)

        touch_telegram.setOnClickListener {
            toast("blb")
        }
        touch_email.setOnClickListener {
            toast("blb")
        }
        touch_gitter.setOnClickListener {
            toast("blb")
        }
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, GetInTouchActivity::class.java)
    }
}
