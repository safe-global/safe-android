package io.gnosis.safe.ui.safe.terms

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class TermsViewModel @Inject constructor(
    private val termsChecker: TermsChecker
) : ViewModel() {

    lateinit var advance: () -> Unit

    val show = MutableLiveData<Boolean>()
    private fun showTermsBottomSheet() {
        show.postValue(true)
    }

    fun checkTerms(advance: () -> Unit) {
        if (termsChecker.getTermsAgreed()) {
            advance()
        } else {
            this.advance = advance
            showTermsBottomSheet()
        }
    }

    fun onAgreeClicked() {
        termsChecker.setTermsAgreed(true)
        advance()
    }
}
