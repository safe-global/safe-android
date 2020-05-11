package io.gnosis.safe.ui.safe.terms

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class TermsViewModel @Inject constructor(
    private val termsChecker: TermsChecker
) : ViewModel() {

    val state = MutableLiveData<ViewAction>()
    private fun showTermsBottomSheet() {
        state.postValue(ViewAction.ShowBottomSheet)
    }

    fun checkTerms() {
        if (termsChecker.getTermsAgreed()) {
            state.postValue(ViewAction.TermsAgreed)
        } else {
            showTermsBottomSheet()
        }
    }

    fun onAgreeClicked() {
        termsChecker.setTermsAgreed(true)
        state.postValue(ViewAction.TermsAgreed)
    }

    interface ViewAction {
        object ShowBottomSheet : ViewAction
        object TermsAgreed : ViewAction
    }
}
