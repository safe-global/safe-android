package pm.gnosis.heimdall.ui.security

import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_security.*
import pm.gnosis.heimdall.GnosisAuthenticatorApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.snackbar
import pm.gnosis.heimdall.ui.base.BaseActivity
import timber.log.Timber
import javax.inject.Inject


class SecurityActivity : BaseActivity() {

    @Inject
    lateinit var viewModelProvider: ViewModelProvider

    private val viewModel by lazy {
        viewModelProvider.get(SecurityContract.ViewModel::class.java)
    }

    private var currentSate = SecurityContract.State.UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_security)
        DaggerViewComponent.builder()
                .applicationComponent(GnosisAuthenticatorApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)

        if (intent?.getBooleanExtra(EXTRA_CLOSE_APP, false) == true) {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_security_submit_button.clicks()
                .flatMap {
                    when (currentSate) {
                        SecurityContract.State.UNINITIALIZED -> Observable.just(
                                SecurityContract.SetupPin(
                                        layout_security_pin_input.text.toString(),
                                        layout_security_repeat_input.text.toString()
                                ))
                        SecurityContract.State.LOCKED -> Observable.just(
                                SecurityContract.Unlock(
                                        layout_security_pin_input.text.toString()
                                ))
                        else ->
                            Observable.empty()
                    }
                }.compose(viewModel.transformer())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setupUi, this::handleError)
    }

    override fun onBackPressed() {
        startActivity(SecurityActivity.createIntentToCloseApp(this))
    }

    private fun setupUi(state: SecurityContract.ViewState) {
        when (state.securityState) {
            SecurityContract.State.UNKNOWN -> hideAll()
            SecurityContract.State.LOCKED -> showUnlockScreen()
            SecurityContract.State.UNINITIALIZED -> showSetupScreen()
            SecurityContract.State.ERROR -> showError()
            SecurityContract.State.UNLOCKED -> finish()
        }
        toggleLoading(state.loading)
        handleNotification(state.notification)
        currentSate = state.securityState
    }

    private fun handleNotification(notification: SecurityContract.Notification?) {
        notification?.let {
            if (it.shouldDisplay()) {
                snackbar(layout_security_content_container, notification.message)
            }
        }
    }

    private fun hideAll() {
        layout_security_error_label.visibility = View.GONE
        layout_security_input_container.visibility = View.GONE
        layout_security_submit_button.visibility = View.GONE
    }

    private fun showSetupScreen() {
        layout_security_error_label.visibility = View.GONE
        layout_security_input_container.visibility = View.VISIBLE
        layout_security_repeat_input.visibility = View.VISIBLE
        layout_security_submit_button.visibility = View.VISIBLE
        layout_security_submit_button.text = getString(R.string.save_pin)
    }

    private fun showUnlockScreen() {
        layout_security_error_label.visibility = View.GONE
        layout_security_input_container.visibility = View.VISIBLE
        layout_security_repeat_input.visibility = View.GONE
        layout_security_submit_button.visibility = View.VISIBLE
        layout_security_submit_button.text = getString(R.string.unlock)
    }

    private fun showError() {
        layout_security_error_label.visibility = View.VISIBLE
        layout_security_input_container.visibility = View.GONE
        layout_security_submit_button.visibility = View.GONE
    }

    private fun toggleLoading(loading: Boolean) {
        layout_security_progress.visibility = if (loading) View.VISIBLE else View.GONE
        layout_security_submit_button.isEnabled = !loading
        layout_security_pin_input.isEnabled = !loading
        layout_security_repeat_input.isEnabled = !loading
    }

    private fun handleError(throwable: Throwable) {
        Timber.d(throwable)
        snackbar(layout_security_content_container, getString(R.string.error_try_again))
    }

    companion object {

        private const val EXTRA_CLOSE_APP = "extra.boolean.close_app"

        fun createIntent(context: Context): Intent {
            return Intent(context, SecurityActivity::class.java)
        }

        private fun createIntentToCloseApp(context: Context): Intent {
            val intent = Intent(context, SecurityActivity::class.java)
            intent.putExtra(EXTRA_CLOSE_APP, true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return intent
        }
    }
}