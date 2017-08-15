package pm.gnosis.android.app.authenticator.ui.base

import android.os.Bundle
import android.support.v4.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import pm.gnosis.android.app.authenticator.GnosisAuthenticatorApplication
import pm.gnosis.android.app.authenticator.di.component.ApplicationComponent

abstract class BaseFragment : Fragment() {
    protected val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(GnosisAuthenticatorApplication[this.context].component)
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    abstract fun inject(component: ApplicationComponent)
}
