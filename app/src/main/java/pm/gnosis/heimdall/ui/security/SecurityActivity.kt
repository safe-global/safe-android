package pm.gnosis.heimdall.ui.security

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.editorActions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Predicate
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.layout_security.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.snackbar
import pm.gnosis.heimdall.common.util.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.utils.filterEditorActions
import timber.log.Timber
import javax.inject.Inject


class SecurityActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: SecurityContract

    private var currentSate = SecurityContract.State.UNKNOWN

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_security)
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)

        if (intent?.getBooleanExtra(EXTRA_CLOSE_APP, false) == true) {
            finish()
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += listen()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { mapButtonClick() }
                .startWith(checkInitialState())
                .subscribeForResult(this::setupUi, this::handleError)
    }

    private fun listen() =
            Observable.merge(
                    layout_security_pin_input.filterEditorActions(EditorInfo.IME_ACTION_DONE),
                    layout_security_repeat_input.filterEditorActions(EditorInfo.IME_ACTION_DONE),
                    layout_security_submit_button.clicks())

    private fun checkInitialState() =
            viewModel.checkState()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { toggleLoading(true) }
                    .doAfterTerminate { toggleLoading(false) }

    private fun mapButtonClick() =
            (when (currentSate) {
                SecurityContract.State.UNINITIALIZED -> viewModel.setupPin(
                        layout_security_pin_input.text.toString(),
                        layout_security_repeat_input.text.toString()
                )
                SecurityContract.State.LOCKED -> viewModel.unlockPin(
                        layout_security_pin_input.text.toString()
                )
                else ->
                    Observable.empty()
            })
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { toggleLoading(true) }
                    .doAfterTerminate { toggleLoading(false) }

    override fun onBackPressed() {
        startActivity(SecurityActivity.createIntentToCloseApp(this))
    }

    private fun setupUi(state: SecurityContract.State) {
        when (state) {
            SecurityContract.State.LOCKED -> showUnlockScreen()
            SecurityContract.State.UNINITIALIZED -> showSetupScreen()
            SecurityContract.State.UNLOCKED -> finish()
            SecurityContract.State.UNKNOWN -> hideAll()
        }
        currentSate = state
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

        if (currentSate == SecurityContract.State.UNKNOWN) {
            showError()
            return
        }

        val message = (throwable as? LocalizedException)?.message ?: getString(R.string.error_try_again)
        snackbar(layout_security_content_container, message)
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