package io.gnosis.safe.ui.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.di.components.ApplicationComponent

abstract class BaseFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject(HeimdallApplication[context!!])
    }


    abstract fun inject(component: ApplicationComponent)
}
