package io.gnosis.safe.ui.safe

import android.content.Context
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Safe
import io.gnosis.safe.ui.base.BaseViewBindingFragment

abstract class SafeOverviewBaseFragment<T> : BaseViewBindingFragment<T>() where T : ViewBinding {

    protected var navHandler: SafeOverviewNavigationHandler? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navHandler = context as SafeOverviewNavigationHandler
    }

    override fun onDetach() {
        super.onDetach()
        navHandler = null
    }

    abstract fun handleActiveSafe(safe: Safe?)
}

interface SafeOverviewNavigationHandler {
    fun setSafeData(safe: Safe?)
}
