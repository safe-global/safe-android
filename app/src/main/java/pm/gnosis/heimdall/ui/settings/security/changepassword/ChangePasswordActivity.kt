package pm.gnosis.heimdall.ui.settings.security.changepassword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_change_password.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.setupToolbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.toast
import javax.inject.Inject

class ChangePasswordActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: ChangePasswordContract

    override fun screenId() = ScreenId.CHANGE_PASSWORD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_change_password)
        setupToolbar(layout_change_password_toolbar)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_change_password_button.clicks()
            .flatMap {
                viewModel.setPassword(
                    layout_change_password_current.text.toString(),
                    layout_change_password_new.text.toString(),
                    layout_change_password_new_repeat.text.toString()
                )
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                toast(R.string.password_change_success)
                finish()
            }, onError = { errorSnackbar(layout_change_password_coordinator, it) })
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build()
            .inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, ChangePasswordActivity::class.java)
    }
}
