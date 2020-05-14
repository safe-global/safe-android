package io.gnosis.safe.ui.splash

import android.app.Application
import android.content.Context
import android.content.Intent
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.test
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.terms.TermsChecker
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences

class SplashViewModelTest {
    private lateinit var preferences: TestPreferences
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var termsChecker: TermsChecker
    private lateinit var context: Context

    @get:Rule
    val instantExecutorRule = TestLifecycleRule()

    @Before
    fun setup() {
        preferences = spyk()
        val application = mockk<Application>().apply {
            every { getSharedPreferences(any(), any()) } returns preferences
        }
        preferencesManager = PreferencesManager(application)
        context = mockk(relaxed = true)
        termsChecker = mockk(relaxed = true)
    }

    @Test
    fun `onStartClicked (terms already agreed) should call advance() and not show bottom sheet`() {
        coEvery { termsChecker.getTermsAgreed() } returns true
        val viewModel = SplashViewModel(termsChecker, appDispatchers, context)

        viewModel.onStartClicked()

        coVerify(exactly = 1) { termsChecker.getTermsAgreed() }
        viewModel.state.test().assertValues(
            SplashViewModel.TermsAgreed(BaseStateViewModel.ViewAction.StartActivity(Intent(context, StartActivity::class.java)))
        )
    }

    @Test
    fun `onStartClicked (terms not agreed previously) should show terms`() {
        coEvery { termsChecker.getTermsAgreed() } returns false
        val viewModel = SplashViewModel(termsChecker, appDispatchers, context)

        viewModel.onStartClicked()

        viewModel.state.test().assertValues(
            SplashViewModel.TermsAgreed(SplashViewModel.ShowTerms)
        )
        coVerify(exactly = 1) { termsChecker.getTermsAgreed() }
    }

    @Test
    fun `onStartClicked (terms not agreed & user clicks agree) should show bottom sheet and call advance()`() {
        coEvery { termsChecker.getTermsAgreed() } returns false

        val viewModel = SplashViewModel(termsChecker, appDispatchers, context)

        viewModel.onStartClicked()

        viewModel.state.test().assertValues(
            SplashViewModel.TermsAgreed(SplashViewModel.ShowTerms)
        )
        coVerify(exactly = 1) { termsChecker.getTermsAgreed() }

        viewModel.handleAgreeClicked()

        viewModel.state.test().assertValues(
            SplashViewModel.TermsAgreed(BaseStateViewModel.ViewAction.StartActivity(Intent(context, StartActivity::class.java)))
        )

    }
}
