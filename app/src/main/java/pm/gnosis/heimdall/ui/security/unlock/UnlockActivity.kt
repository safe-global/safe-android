package pm.gnosis.heimdall.ui.security.unlock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.editorActions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_unlock.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.errorToast
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.svalinn.security.FingerprintUnlockFailed
import pm.gnosis.svalinn.security.FingerprintUnlockHelp
import pm.gnosis.svalinn.security.FingerprintUnlockResult
import pm.gnosis.svalinn.security.FingerprintUnlockSuccessful
import timber.log.Timber

class UnlockActivity : ViewModelActivity<UnlockContract>() {
    override fun screenId() = ScreenId.UNLOCK

    private var fingerPrintDisposable: Disposable? = null

    private var forceConfirmCredentials: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)

        forceConfirmCredentials = intent?.getBooleanExtra(EXTRA_CONFIRM_CREDENTIALS, false) ?: false
        if (intent?.getBooleanExtra(EXTRA_CLOSE_APP, false) == true) {
            finish()
        }

        layout_unlock_password_input.disableAccessibility()
    }

    override fun onStart() {
        super.onStart()
        fingerPrintDisposable = encryptionManager.isFingerPrintSet()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess {
                layout_unlock_fingerprint_group.visible(it)
                layout_unlock_password_input.visible(!it)
            }
            .flatMapObservable {
                if (it) viewModel.observeFingerprint() else Observable.empty()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onFingerprintResult, onError = ::onFingerprintError)

        disposables += viewModel.checkState(forceConfirmCredentials)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onState, onError = ::onStateCheckError)

        disposables += layout_unlock_password_input.editorActions()
            .filter { it == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || it == android.view.inputmethod.EditorInfo.IME_NULL }
            .flatMap { viewModel.unlock(layout_unlock_password_input.text.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::onState, onError = ::onUnlockError)

        disposables += layout_unlock_switch_to_password.clicks()
            .subscribeBy(onNext = {
                fingerPrintDisposable?.dispose()
                layout_unlock_fingerprint_group.visible(false)
                layout_unlock_password_input.visible(true)
                layout_unlock_password_input.showKeyboardForView()
            }, onError = Timber::e)
    }

    private fun onFingerprintResult(result: FingerprintUnlockResult) {
        when (result) {
            is FingerprintUnlockSuccessful -> onState(UnlockContract.State.UNLOCKED)
            is FingerprintUnlockFailed -> {
                vibrate(200)
                snackbar(layout_unlock_coordinator, R.string.fingerprint_not_recognized, Snackbar.LENGTH_SHORT)
            }
            is FingerprintUnlockHelp -> {
                result.message?.let { snackbar(layout_unlock_coordinator, it, Snackbar.LENGTH_SHORT) }
            }
        }
    }

    private fun onFingerprintError(throwable: Throwable) {
        Timber.e(throwable)
        layout_unlock_fingerprint_group.visible(false)
        layout_unlock_password_input.visible(true)
        onStateCheckError(throwable)
    }

    private fun onState(state: UnlockContract.State) {
        when (state) {
            UnlockContract.State.UNINITIALIZED -> startActivity(createIntentToCloseApp(this))
            UnlockContract.State.UNLOCKED -> {
                viewModel.syncPushAuthentication()
                setResult(Activity.RESULT_OK)
                finish()
            }
            UnlockContract.State.LOCKED -> {
            }
        }
    }

    override fun onStop() {
        fingerPrintDisposable?.dispose()
        super.onStop()
    }

    override fun onBackPressed() {
        if (forceConfirmCredentials) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else {
            startActivity(UnlockActivity.createIntentToCloseApp(this))
        }
    }

    private fun onStateCheckError(throwable: Throwable) {
        errorSnackbar(layout_unlock_coordinator, throwable)
    }

    private fun onUnlockError(throwable: Throwable) {
        Timber.e(throwable)
        errorToast(throwable)
    }

    override fun layout() = R.layout.layout_unlock

    override fun inject(component: ViewComponent) = component.inject(this)

    companion object {
        private const val EXTRA_CLOSE_APP = "extra.boolean.close_app"
        private const val EXTRA_CONFIRM_CREDENTIALS = "extra.boolean.confirm_credentials"

        fun createIntent(context: Context) = Intent(context, UnlockActivity::class.java).noHistory()

        fun createConfirmIntent(context: Context) = createIntent(context).apply {
            putExtra(EXTRA_CONFIRM_CREDENTIALS, true)
        }

        private fun createIntentToCloseApp(context: Context): Intent {
            val intent = Intent(context, UnlockActivity::class.java)
            intent.putExtra(EXTRA_CLOSE_APP, true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return intent
        }
    }
}
