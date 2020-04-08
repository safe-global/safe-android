package io.gnosis.safe.ui.base

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.di.components.ViewComponent
import javax.inject.Inject

abstract class ViewModelActivity<VM : ViewModel, VB: ViewBinding> : BaseActivity() {
    @Inject
    lateinit var viewModel: VM

    abstract fun inflateBinding(): VB

    abstract fun inject(component: ViewComponent)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(viewComponent())
        setContentView(inflateBinding().root)
    }
}
