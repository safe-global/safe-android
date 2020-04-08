package io.gnosis.safe.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.di.components.ApplicationComponent

abstract class BaseFragment<T> : Fragment()
        where T : ViewBinding {

    private var _binding: T? = null
    protected val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(HeimdallApplication[context!!])
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun inject(component: ApplicationComponent)

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T
}
