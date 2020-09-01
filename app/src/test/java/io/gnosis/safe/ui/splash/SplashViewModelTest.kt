package io.gnosis.safe.ui.splash

import android.app.Application
import android.content.Context
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.TestLifecycleRule
import io.gnosis.safe.Tracker
import io.gnosis.safe.appDispatchers
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.test
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.terms.TermsChecker
import io.mockk.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.tests.utils.TestPreferences

class SplashViewModelTest {

    private val tracker: Tracker = mockk()
    private val notificationRepository = mockk<NotificationRepository>()
    private val safeRepository = mockk<SafeRepository>()
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
    fun `onStartClicked (terms already agreed) should emit StartActivity`() {
        coEvery { termsChecker.getTermsAgreed() } returns true
        val viewModel = SplashViewModel(notificationRepository, safeRepository, tracker, termsChecker, appDispatchers, context)

        viewModel.onStartClicked()

        with(viewModel.state.test().values()) {
            assertEquals(1, size)
            assert(this[0].viewAction is BaseStateViewModel.ViewAction.StartActivity)
        }
        coVerify(exactly = 1) { termsChecker.getTermsAgreed() }
    }

    @Test
    fun `onStartClicked (terms not agreed previously) should emit ShowTerms`() {
        coEvery { termsChecker.getTermsAgreed() } returns false
        val viewModel = SplashViewModel(notificationRepository, safeRepository, tracker, termsChecker, appDispatchers, context)

        viewModel.onStartClicked()

        viewModel.state.test().assertValues(
            SplashViewModel.TermsAgreed(SplashViewModel.ShowTerms)
        )
        coVerify(exactly = 1) { termsChecker.getTermsAgreed() }
    }

    @Test
    fun `handleAgreeClicked (user clicks agree) should emit StartActivity`() {
        val viewModel = SplashViewModel(notificationRepository, safeRepository, tracker, termsChecker, appDispatchers, context)

        viewModel.handleAgreeClicked()

        with(viewModel.state.test().values()) {
            assertEquals(1, size)
            assert(this[0].viewAction is BaseStateViewModel.ViewAction.StartActivity)
        }
        coVerify(exactly = 1) { termsChecker.setTermsAgreed(true) }
    }

    @Test
    fun `skipSplashScreen (terms not agreed previously) should make get started button visible`() {
        coEvery { termsChecker.getTermsAgreed() } returns false
        val viewModel = SplashViewModel(notificationRepository, safeRepository, tracker, termsChecker, appDispatchers, context)

        viewModel.skipGetStartedButtonWhenTermsAgreed()

        with(viewModel.state.test().values()) {
            assertEquals(1, size)
            assert(this[0].viewAction is SplashViewModel.ShowButton)
        }
        coVerify(exactly = 1) { termsChecker.getTermsAgreed() }
    }

    @Test
    fun `skipSplashScreen (terms agreed previously) should emit StartActivity`() {
        coEvery { termsChecker.getTermsAgreed() } returns true
        val viewModel = SplashViewModel(notificationRepository, safeRepository, tracker, termsChecker, appDispatchers, context)

        viewModel.skipGetStartedButtonWhenTermsAgreed()

        with(viewModel.state.test().values()) {
            assertEquals(1, size)
            assert(this[0].viewAction is BaseStateViewModel.ViewAction.StartActivity)
        }
        coVerify(exactly = 1) { termsChecker.getTermsAgreed() }
    }

    @Test
    fun `onAppStart should register notification service and set user properties`() = runBlockingTest {

        coEvery { notificationRepository.register() } just Runs
        coEvery { notificationRepository.clearNotifications() } just Runs
        coEvery { notificationRepository.checkPermissions() } returns true
        coEvery { tracker.setPushInfo(any()) } just Runs
        coEvery { tracker.setNumSafes(any()) } just Runs
        coEvery { safeRepository.getSafeCount() } returns 0

        val viewModel = SplashViewModel(notificationRepository, safeRepository, tracker, termsChecker, appDispatchers, context)

        viewModel.onAppStart()

        coVerifySequence {
            notificationRepository.register()
            notificationRepository.clearNotifications()
            notificationRepository.checkPermissions()
            tracker.setPushInfo(true)
            tracker.setNumSafes(0)
        }
    }
}
