package io.gnosis.heimdall.ui.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.gnosis.heimdall.HeimdallApplication
import io.gnosis.heimdall.di.components.ApplicationComponent

abstract class BaseFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(HeimdallApplication[context!!])
    }


    abstract fun inject(component: ApplicationComponent)
}
