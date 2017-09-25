package pm.gnosis.heimdall.ui.base

import android.os.Bundle
import android.support.v4.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import pm.gnosis.heimdall.GnosisAuthenticatorApplication
import pm.gnosis.heimdall.common.di.component.ApplicationComponent

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
