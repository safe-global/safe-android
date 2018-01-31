package pm.gnosis.heimdall.ui.security.unlock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.*
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_unlock.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.*
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.security.FingerprintUnlockFailed
import pm.gnosis.heimdall.security.FingerprintUnlockHelp
import pm.gnosis.heimdall.security.FingerprintUnlockResult
import pm.gnosis.heimdall.security.FingerprintUnlockSuccessful
import pm.gnosis.heimdall.ui.base.SecuredBaseActivity
import pm.gnosis.heimdall.utils.disableAccessibility
import pm.gnosis.heimdall.utils.errorToast
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UnlockActivity : SecuredBaseActivity() {
    override fun screenId() = ScreenId.UNLOCK

    @Inject
    lateinit var viewModel: UnlockContract

    private val nextClickSubject = PublishSubject.create<Unit>()

    private var handleRotation = 0f

    private var fingerPrintDisposable: Disposable? = null

    private var forceConfirmCredentials: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_unlock)

        forceConfirmCredentials = intent?.getBooleanExtra(EXTRA_CONFIRM_CREDENTIALS, false) ?: false
        if (intent?.getBooleanExtra(EXTRA_CLOSE_APP, false) == true) {
            finish()
        }

        layout_unlock_password_input.disableAccessibility()
        layout_unlock_password_input.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE, EditorInfo.IME_NULL ->
                    nextClickSubject.onNext(Unit)
            }
            true
        }
    }

    override fun onWindowObscured() {
        super.onWindowObscured()
        // Window is obscured, clear input and disable to prevent potential leak
        layout_unlock_password_input.text = null
        layout_unlock_password_input.isEnabled = false
    }

    override fun onStart() {
        super.onStart()
        fingerPrintDisposable = encryptionManager.isFingerPrintSet()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess {
                    layout_unlock_fingerprint.visible(it)
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

        disposables += nextClickSubject
                .flatMap { viewModel.unlock(layout_unlock_password_input.text.toString()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = ::onState, onError = ::onUnlockError)

        disposables += layout_unlock_password_input.textChanges()
                .window(200, TimeUnit.MILLISECONDS)
                .flatMapMaybe { it.lastElement() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    animateHandle(it.length * 15f, interpolator = DecelerateInterpolator())
                }, Timber::e)

        disposables += layout_unlock_switch_to_password.clicks()
                .subscribeBy(onNext = {
                    fingerPrintDisposable?.dispose()
                    layout_unlock_fingerprint.visibility = View.GONE
                    layout_unlock_password_input.visibility = View.VISIBLE
                    layout_unlock_password_input.showKeyboardForView()
                }, onError = Timber::e)
    }

    private fun onFingerprintResult(result: FingerprintUnlockResult) {
        layout_unlock_error.visibility = View.INVISIBLE
        when (result) {
            is FingerprintUnlockSuccessful -> onState(UnlockContract.State.UNLOCKED)
            is FingerprintUnlockFailed -> {
                vibrate(200)
                animateHandleOnError()
                layout_unlock_error.visibility = View.VISIBLE
                layout_unlock_error.text = getString(R.string.fingerprint_not_recognized)
            }
            is FingerprintUnlockHelp -> {
                result.message?.let {
                    layout_unlock_error.visibility = View.VISIBLE
                    layout_unlock_error.text = it
                }
            }
        }
    }

    private fun onFingerprintError(throwable: Throwable) {
        Timber.e(throwable)
        animateHandleOnError()
        layout_unlock_fingerprint.visibility = View.GONE
        layout_unlock_password_input.visibility = View.VISIBLE
        onStateCheckError(throwable)
    }

    private fun onState(state: UnlockContract.State) {
        when (state) {
            UnlockContract.State.UNINITIALIZED -> startActivity(createIntentToCloseApp(this))
            UnlockContract.State.UNLOCKED -> animateHandle(handleRotation + 360f, {
                setResult(Activity.RESULT_OK)
                finish()
            })
            UnlockContract.State.LOCKED -> {
            }
        }
    }

    private fun animateHandle(rotation: Float, endAction: () -> Unit = {}, interpolator: Interpolator = LinearInterpolator()) {
        handleRotation = rotation
        layout_unlock_handle.clearAnimation()
        layout_unlock_handle.animate()
                .rotation(handleRotation)
                .setDuration(300)
                .setInterpolator(interpolator)
                .withEndAction(endAction)
    }

    override fun onStop() {
        layout_unlock_handle.clearAnimation()
        layout_unlock_handle.animate().cancel()
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
        errorToast(throwable)
    }

    private fun onUnlockError(throwable: Throwable) {
        Timber.e(throwable)
        animateHandleOnError()
        errorToast(throwable)
    }

    private fun animateHandleOnError() {
        val rotationAnimation = RotateAnimation(0f, 20f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
                .apply {
                    duration = 50
                    repeatCount = 3
                    repeatMode = Animation.REVERSE
                }
        layout_unlock_handle.startAnimation(rotationAnimation)
    }

    private fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

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
