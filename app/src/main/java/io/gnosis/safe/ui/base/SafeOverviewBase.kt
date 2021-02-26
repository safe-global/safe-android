package io.gnosis.safe.ui.base

import android.content.Context
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.snackbar

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

    override fun onResume() {
        super.onResume()
        if (ownerImported()) {
            snackbar(requireView(), getString(R.string.signing_owner_key_imported))
            resetOwnerImported()
        }
        if (rejectionConfirmed()) {
            resetRejectionConfirmed()
            snackbar(requireView(), R.string.rejection_successfully_submitted)
        }
    }

    abstract fun handleActiveSafe(safe: Safe?)

    private fun ownerImported(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(OWNER_IMPORT_RESULT) == true
    }

    private fun resetOwnerImported() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(OWNER_IMPORT_RESULT, false)
    }

    private fun rejectionConfirmed(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(REJECTION_CONFIRMATION_RESULT) == true
    }

    private fun resetRejectionConfirmed() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(REJECTION_CONFIRMATION_RESULT, false)
    }

    companion object {
        const val OWNER_IMPORT_RESULT = "args.boolean.owner_import_result"
        const val REJECTION_CONFIRMATION_RESULT = "args.boolean.rejection_confirmation_result"
    }
}

interface SafeOverviewNavigationHandler {
    fun setSafeData(safe: Safe?)
}
