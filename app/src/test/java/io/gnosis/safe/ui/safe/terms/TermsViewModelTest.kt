package io.gnosis.safe.ui.safe.terms

import android.app.Application
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.TestLiveDataObserver
import io.gnosis.safe.ui.safe.terms.TermsViewModel.ViewAction
import io.gnosis.safe.ui.safe.terms.TermsViewModel.ViewAction.ShowBottomSheet
import io.gnosis.safe.ui.safe.terms.TermsViewModel.ViewAction.TermsAgreed
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import junit.framework.Assert.assertFalse
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
    val observer = TestLiveDataObserver<ViewAction>()

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
        every { termsChecker.getTermsAgreed() } returns true
        val viewModel = TermsViewModel(termsChecker)
        viewModel.state.observeForever(observer)

        viewModel.checkTerms()

        observer.assertValues(TermsAgreed)
    }

    @Test
    fun `checkTerms (terms not agreed previously AND agree button not hit) should not call advance()`() {
        termsChecker = mockk(relaxed = true)
        every { termsChecker.getTermsAgreed() } returns false
        val viewModel = TermsViewModel(termsChecker)
        viewModel.state.observeForever(observer)

        viewModel.checkTerms()

        observer.assertValues(ShowBottomSheet)
    }

    @Test
    fun `checkTerms (user clicks agree) should show bottom cheet and call advance()`() {
        termsChecker = TermsChecker(preferencesManager)
        assertFalse(termsChecker.getTermsAgreed())

        val viewModel = TermsViewModel(termsChecker)
        viewModel.state.observeForever(observer)

        viewModel.checkTerms()
        viewModel.onAgreeClicked()

        observer.assertValues(ShowBottomSheet, TermsAgreed)
    }
}
