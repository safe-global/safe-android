package pm.gnosis.heimdall.ui.settings.general.changepassword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_password_change.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.settings.general.changepassword.ChangePasswordContract.State.*
import pm.gnosis.heimdall.views.TwoStepPanel
import timber.log.Timber
import javax.inject.Inject


class PasswordChangeActivity : ViewModelActivity<ChangePasswordContract>() {

    @Inject
    lateinit var toolbarHelper: ToolbarHelper

    override fun screenId() = ScreenId.SETTINGS_CHANGE_PASSWORD

    override fun layout(): Int = R.layout.layout_password_change

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.layout_password_change_content, ChangePasswordEnterOldFragment.newInstance())
                .commit()
        }

        layout_password_change_panel.disabled = false
        layout_password_change_toolbar_nav_icon.setOnClickListener {
            onDismiss()
        }
    }


    override fun onStart() {
        super.onStart()

        disposables += layout_password_change_panel.forwardClicks
            .subscribeBy(onNext = { viewModel.confirm() }, onError = Timber::e)

        disposables += viewModel.state().observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it.state) {
                    ENTER_OLD_PASSWORD -> {
                        layout_password_change_panel.step = TwoStepPanel.Step.ONE
                        layout_password_change_panel.forwardLabel = getString(R.string.next)
                    }
                    INVALID_PASSWORD -> {

                    }
                    ENTER_NEW_PASSWORD -> {
                        if (layout_password_change_panel.step == TwoStepPanel.Step.ONE) {
                            layout_password_change_panel.step = TwoStepPanel.Step.TWO
                            layout_password_change_panel.forwardLabel = getString(R.string.save)

                            supportFragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                                .replace(R.id.layout_password_change_content, ChangePasswordEnterNewFragment.newInstance())
                                .commit()
                        }
                    }
                    PASSWORD_CHANGED -> onDismiss()
                }
                layout_password_change_panel.disabled = !it.confirmEnabled
            }

        //disposables += toolbarHelper.setupShadow(layout_password_change_toolbar_shadow, dialog_change_password_content_scroll)
    }

    private fun onDismiss() {
        finish()
    }

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {

        fun createIntent(context: Context) = Intent(context, PasswordChangeActivity::class.java)
    }
}