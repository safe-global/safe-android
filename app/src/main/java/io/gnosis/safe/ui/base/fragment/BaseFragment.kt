package io.gnosis.safe.ui.base.fragment

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.ScreenId
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.di.modules.ViewModule
import kotlinx.coroutines.launch
import java.math.BigInteger
import javax.inject.Inject

abstract class BaseFragment : Fragment() {

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var safeRepo: SafeRepository

    override fun onAttach(context: Context) {
        super.onAttach(context)
        HeimdallApplication[requireContext()].inject(this)
    }

    override fun onResume() {
        super.onResume()
        screenId()?.let {
            lifecycleScope.launch {
                tracker.logScreen(it, chainId())
            }
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

    protected open suspend fun chainId(): BigInteger? {
        return safeRepo.getActiveSafe()?.chainId
    }
}
