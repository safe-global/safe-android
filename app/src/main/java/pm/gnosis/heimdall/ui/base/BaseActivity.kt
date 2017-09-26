package pm.gnosis.heimdall.ui.base

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.ui.security.SecurityActivity
import timber.log.Timber
import javax.inject.Inject

abstract class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var encryptionManager: EncryptionManager

    protected val disposables = CompositeDisposable()

    private var performSecurityCheck = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HeimdallApplication[this].component.inject(this)
    }

    override fun onStart() {
        super.onStart()
        if (performSecurityCheck) {
            disposables += encryptionManager.unlocked()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::checkSecurity, this::handleCheckError)
        }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    protected fun skipSecurityCheck() {
        performSecurityCheck = false
    }

    private fun checkSecurity(unlocked: Boolean) {
        if (!unlocked) {
            startActivity(SecurityActivity.createIntent(this))
        }
    }

    private fun handleCheckError(throwable: Throwable) {
        Timber.d(throwable)
        // Show blocker screen. No auth -> no app usage
    }

}