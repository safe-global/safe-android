package io.gnosis.safe.ui.safe.terms

import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class TermsViewModel @Inject constructor(
    private val termsChecker: TermsChecker,
    appDispatchers: AppDispatchers

) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    private fun showTermsBottomSheet() {
        safeLaunch {
            updateState { TermsOfUseState(ViewAction.ShowBottomSheet) }
        }
    }

    fun checkTerms() {
        safeLaunch {
            if (termsChecker.getTermsAgreed()) {
                updateState { TermsOfUseState(ViewAction.TermsAgreed) }
            } else {
                showTermsBottomSheet()
            }
        }
    }

    fun onAgreeClicked() {
        safeLaunch {
            termsChecker.setTermsAgreed(true)
            updateState { TermsOfUseState(ViewAction.TermsAgreed) }
        }
    }

    interface ViewAction {
        object ShowBottomSheet : BaseStateViewModel.ViewAction
        object TermsAgreed : BaseStateViewModel.ViewAction
    }

    override fun initialState(): State = TermsOfUseState(BaseStateViewModel.ViewAction.Loading(false))

    data class TermsOfUseState(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : State

}
