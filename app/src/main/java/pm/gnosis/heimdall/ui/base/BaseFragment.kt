package pm.gnosis.heimdall.ui.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.di.components.ApplicationComponent

abstract class BaseFragment : Fragment() {
    protected val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(HeimdallApplication[context!!])
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    abstract fun inject(component: ApplicationComponent)
}
