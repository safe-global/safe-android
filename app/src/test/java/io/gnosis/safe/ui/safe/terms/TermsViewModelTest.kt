package io.gnosis.safe.ui.safe.terms

import android.app.Application
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences

class TermsViewModelTest {

    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var termsChecker: TermsChecker

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)
    }

    @Test
    fun `checkTerms (terms already agreed) should call advance() and not show bottom sheet`() {
        termsChecker = mockk(relaxed = true)
        coEvery { termsChecker.getTermsAgreed() } returns true
        val viewModel = TermsViewModel(termsChecker, appDispatchers)

        viewModel.checkTerms()

        viewModel.state.test().assertValues(
            TermsViewModel.TermsOfUseState(TermsViewModel.ViewAction.TermsAgreed)
        )
    }

    @Test
    fun `checkTerms (terms not agreed previously AND agree button not hit) should not call advance()`() {
        termsChecker = mockk(relaxed = true)
        coEvery { termsChecker.getTermsAgreed() } returns false
        val viewModel = TermsViewModel(termsChecker, appDispatchers)

        viewModel.checkTerms()

        viewModel.state.test().assertValues(
            TermsViewModel.TermsOfUseState(TermsViewModel.ViewAction.ShowBottomSheet)
        )
    }

    @Test
    fun `checkTerms (user clicks agree) should show bottom sheet and call advance()`() {
        termsChecker = mockk(relaxed = true)
        coEvery { termsChecker.getTermsAgreed() } returns false

        val viewModel = TermsViewModel(termsChecker, appDispatchers)

        viewModel.checkTerms()

        viewModel.state.test().assertValues(
            TermsViewModel.TermsOfUseState(TermsViewModel.ViewAction.ShowBottomSheet)
        )

        viewModel.onAgreeClicked()

        viewModel.state.test().assertValues(
            TermsViewModel.TermsOfUseState(TermsViewModel.ViewAction.TermsAgreed)
        )
    }
}
