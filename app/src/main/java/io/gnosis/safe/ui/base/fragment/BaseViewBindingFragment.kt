package io.gnosis.safe.ui.base.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class BaseViewBindingFragment<T> : BaseFragment()
        where T : ViewBinding {

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): T
}
