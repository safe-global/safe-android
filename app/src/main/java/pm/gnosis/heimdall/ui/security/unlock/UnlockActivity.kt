package pm.gnosis.heimdall.ui.security.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.*
import android.view.inputmethod.EditorInfo
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_unlock.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.utils.errorToast
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UnlockActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: UnlockContract

    private val nextClickSubject = PublishSubject.create<Unit>()

    private var handleRotation = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_unlock)

        if (intent?.getBooleanExtra(EXTRA_CLOSE_APP, false) == true) {
            finish()
        }

        layout_unlock_password_input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                nextClickSubject.onNext(Unit)
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.checkState()
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
    }

    private fun onState(state: UnlockContract.State) {
        when (state) {
            UnlockContract.State.UNINITIALIZED -> startActivity(createIntentToCloseApp(this))
            UnlockContract.State.UNLOCKED -> animateHandle(handleRotation + 360f, { finish() })
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
        super.onStop()
    }

    override fun onBackPressed() {
        startActivity(UnlockActivity.createIntentToCloseApp(this))
    }

    private fun onStateCheckError(throwable: Throwable) {
        errorToast(throwable)
    }

    private fun onUnlockError(throwable: Throwable) {
        val rotationAnimation = RotateAnimation(0f, 20f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
                .apply {
                    duration = 50
                    repeatCount = 3
                    repeatMode = Animation.REVERSE
                }
        layout_unlock_handle.startAnimation(rotationAnimation)
        errorToast(throwable)
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

        fun createIntent(context: Context) = Intent(context, UnlockActivity::class.java)

        private fun createIntentToCloseApp(context: Context): Intent {
            val intent = Intent(context, UnlockActivity::class.java)
            intent.putExtra(EXTRA_CLOSE_APP, true)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return intent
        }
    }
}
