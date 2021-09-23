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
        if (ownerCreated()) {
            snackbar(requireView(), getString(R.string.signing_owner_key_created))
            resetOwnerCreated()
        }
        if (rejectionConfirmed()) {
            resetRejectionConfirmed()
            snackbar(requireView(), R.string.rejection_successfully_submitted)
        }
        if (passcodeSet()) {
            resetPasscodeSet()
            snackbar(requireView(), R.string.passcode_created)
        }
        if (passcodeDisabled()) {
            resetPasscodeDisabled()
            snackbar(requireView(), R.string.passcode_disabled)
        }
        if (passcodeChanged()) {
            resetPasscodeChanged()
            snackbar(requireView(), R.string.passcode_changed)
        }
    }

    abstract fun handleActiveSafe(safe: Safe?)

    private fun ownerImported(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(OWNER_IMPORT_RESULT) == true
    }

    private fun ownerCreated(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(OWNER_CREATE_RESULT) == true
    }

    private fun resetOwnerImported() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(OWNER_IMPORT_RESULT, false)
    }

    private fun resetOwnerCreated() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(OWNER_CREATE_RESULT, false)
    }

    private fun rejectionConfirmed(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(REJECTION_CONFIRMATION_RESULT) == true
    }

    private fun resetRejectionConfirmed() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(REJECTION_CONFIRMATION_RESULT, false)
    }

    private fun passcodeSet(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(PASSCODE_SET_RESULT) == true
    }

    private fun resetPasscodeSet() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(PASSCODE_SET_RESULT, false)
    }

    private fun passcodeDisabled(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(PASSCODE_DISABLED_RESULT) == true
    }

    private fun resetPasscodeDisabled() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(PASSCODE_DISABLED_RESULT, false)
    }

    private fun passcodeChanged(): Boolean {
        return findNavController().currentBackStackEntry?.savedStateHandle?.get<Boolean>(PASSCODE_CHANGED_RESULT) == true
    }

    private fun resetPasscodeChanged() {
        findNavController().currentBackStackEntry?.savedStateHandle?.set(PASSCODE_CHANGED_RESULT, false)
    }

    companion object {
        const val OWNER_IMPORT_RESULT = "args.boolean.owner_import_result"
        const val OWNER_CREATE_RESULT = "args.boolean.owner_create_result"
        const val OWNER_SELECTED_RESULT = "args.string.owner_selected_result"
        const val OWNER_SIGNED_RESULT = "args.string.owner_signed_result"
        const val PASSCODE_SET_RESULT = "args.boolean.passcode_set_result"
        const val PASSCODE_DISABLED_RESULT = "args.boolean.passcode_disabled_result"
        const val PASSCODE_CHANGED_RESULT = "args.boolean.passcode_changed_result"
        const val REJECTION_CONFIRMATION_RESULT = "args.boolean.rejection_confirmation_result"
    }
}

interface SafeOverviewNavigationHandler {
    fun setSafeData(safe: Safe?)
}
