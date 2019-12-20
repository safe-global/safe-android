package pm.gnosis.heimdall.ui.two_factor.keycard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.base.BaseStateViewModel

abstract class KeycardBaseFragment<S: BaseStateViewModel.State, T: BaseStateViewModel<S>>: Fragment() {
    abstract var viewModel: T

    abstract val layout: Int

    abstract fun inject(component: ViewComponent)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(layout, container, false)

    abstract fun updateState(state: S)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(
            DaggerViewComponent.builder()
                .viewModule(ViewModule(context!!, parentFragment))
                .applicationComponent(HeimdallApplication[context!!])
                .build()
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state.observe(this, Observer { updateState(it) })
    }
}
