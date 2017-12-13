package pm.gnosis.heimdall.ui.security.unlock

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_unlock.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.startActivity
import pm.gnosis.heimdall.common.utils.subscribeForResult
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.utils.errorToast
import javax.inject.Inject

class UnlockActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: UnlockContract

    private val nextClickSubject = PublishSubject.create<Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        skipSecurityCheck()
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_unlock)

        layout_unlock_password.setOnEditorActionListener { _, actionId, _ ->
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
                .flatMap { viewModel.unlock(layout_unlock_password.text.toString()) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = ::onState, onError = ::onUnlockError)
    }

    private fun onState(state: UnlockContract.State) {
        when (state) {
            UnlockContract.State.UNINITIALIZED -> startActivity(PasswordSetupActivity.createIntent(this), noHistory = true)
            UnlockContract.State.UNLOCKED -> finish()
            UnlockContract.State.LOCKED -> {
            }
        }
    }

    override fun onBackPressed() {
        startActivity(UnlockActivity.createIntentToCloseApp(this))
    }

    private fun onStateCheckError(throwable: Throwable) {
        errorToast(throwable)
    }

    private fun onUnlockError(throwable: Throwable) {
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
