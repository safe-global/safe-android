package io.gnosis.safe.ui.base

import android.content.Context
import androidx.fragment.app.Fragment
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.ScreenId
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.di.modules.ViewModule
import javax.inject.Inject

abstract class BaseFragment : Fragment() {

    @Inject
    lateinit var tracker: Tracker

    override fun onAttach(context: Context) {
        super.onAttach(context)
        HeimdallApplication[requireContext()].inject(this)
    }

    override fun onStart() {
        super.onStart()
        screenId()?.let {
            tracker.setCurrentScreenId(requireActivity(), it)
        }
    }

    protected fun buildViewComponent(context: Context): ViewComponent =
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[context])
            .viewModule(ViewModule(context, viewModelProvider()))
            .build()

    abstract fun screenId(): ScreenId?

    abstract fun inject(component: ViewComponent)

    protected open fun viewModelProvider(): Any? = parentFragment
}
