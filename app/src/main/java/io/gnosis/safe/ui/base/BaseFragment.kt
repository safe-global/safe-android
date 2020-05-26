package io.gnosis.safe.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.ScreenId
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.di.modules.ViewModule
import javax.inject.Inject

abstract class BaseFragment<T> : Fragment()
        where T : ViewBinding {

    @Inject
    lateinit var tracker: Tracker

    private var _binding: T? = null
    protected val binding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inject(buildViewComponent(context))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        screenId()?.let {
            tracker.setCurrentScreenId(requireActivity(), it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildViewComponent(context: Context): ViewComponent =
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[context])
            .viewModule(ViewModule(context, viewModelProvider()))
            .build()

    abstract fun screenId(): ScreenId?

    abstract fun inject(component: ViewComponent)

    open protected fun viewModelProvider(): Any? = parentFragment

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T
}
