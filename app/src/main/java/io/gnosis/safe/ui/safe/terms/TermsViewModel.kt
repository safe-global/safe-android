package io.gnosis.safe.ui.safe.terms

import androidx.lifecycle.ViewModel
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import javax.inject.Inject

class TermsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    lateinit var termsBottomSheetDialog: TermsBottomSheetDialog

    private fun setTermsAgreed(value: Boolean) {
        preferencesManager.prefs.edit {
            putBoolean(TERMS_AGREED, value)
        }
    }

    private fun getTermsAgreed(): Boolean = preferencesManager.prefs.getBoolean(TERMS_AGREED, false)

    fun checkTerms(advance: () -> Unit) {
        if (getTermsAgreed()) {
            advance()
        } else {
            termsBottomSheetDialog.onUserClicksAgree = {
                advance()
                setTermsAgreed(true)
            }
            termsBottomSheetDialog.show()
        }
    }

    companion object {
        const val TERMS_AGREED = "prefs.boolean.terms_agreed"
    }
}
