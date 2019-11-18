package pm.gnosis.heimdall.ui.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.lifecycle.ViewModel
import pm.gnosis.heimdall.di.components.ViewComponent
import javax.inject.Inject

abstract class ViewModelActivity<VM : ViewModel> : BaseActivity() {
    @Inject
    lateinit var viewModel: VM

    @LayoutRes
    abstract fun layout(): Int

    abstract fun inject(component: ViewComponent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(viewComponent())
        setContentView(layout())
    }
}
