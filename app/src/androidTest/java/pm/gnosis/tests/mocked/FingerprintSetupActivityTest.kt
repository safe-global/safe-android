package pm.gnosis.tests.mocked

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.matcher.RootMatchers.withDecorView
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupActivity
import pm.gnosis.heimdall.ui.onboarding.fingerprint.FingerprintSetupContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.svalinn.common.utils.clearStack
import pm.gnosis.tests.BaseUiTest
import pm.gnosis.tests.utils.UIMockUtils
import pm.gnosis.tests.utils.matchesIntentExactly


@RunWith(AndroidJUnit4::class)
class FingerprintSetupActivityTest : BaseUiTest() {
    @JvmField
    @Rule
    val activityRule = ActivityTestRule(FingerprintSetupActivity::class.java, true, false)

    private val addressBookRepoMock = mock(AddressBookRepository::class.java)

    // ViewModel mocks

    private val fingerprintSetupContract = mock(FingerprintSetupContract::class.java)

    private val safeMainContract = mock(SafeMainContract::class.java)

    @Before
    fun setup() {
        Intents.init()
        val comp = setupBaseInjects()
        given(comp.addressHelper()).willReturn(AddressHelper(addressBookRepoMock))
        given(comp.toolbarHelper()).willReturn(ToolbarHelper())
        given(comp.viewModelFactory()).willReturn(object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    FingerprintSetupContract::class.java -> fingerprintSetupContract
                    SafeMainContract::class.java -> safeMainContract
                    else -> null
                } as T
            }
        })
        // This is part of the base activity and should not be tested here -> assume that it is always unlocked
        given(encryptionManagerMock.unlocked()).willReturn(Single.just(true))
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun initialState() {
        // We don't want to emit a value
        given(fingerprintSetupContract.observeFingerprintForSetup()).willReturn(PublishSubject.create())
        val activity = activityRule.launchActivity(null)

        // Bottom Bar
        onView(withId(R.id.layout_fingerprint_setup_continue)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_fingerprint_setup_bottom_bar)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_fingerprint_setup_next_arrow)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_fingerprint_setup_continue_label)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.skip))))

        // Title
        onView(withId(R.id.layout_fingerprint_setup_title)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.setup_fingerprint))))
        // Scrollable content
        onView(withId(R.id.layout_fingerprint_setup_image)).check(matches(isDisplayed()))
        onView(withId(R.id.layout_fingerprint_setup_info_title)).check(matches(allOf(isDisplayed(), withText(R.string.setup_fingerprint_place))))
        onView(withId(R.id.layout_fingerprint_setup_description)).check(matches(allOf(isDisplayed(), withText(R.string.setup_fingerprint_description))))

        then(encryptionManagerMock).should().unlocked()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        // Contract interaction
        then(fingerprintSetupContract).should().observeFingerprintForSetup()
        then(fingerprintSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.FINGERPRINT))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.FINGERPRINT)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(FingerprintSetupActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun skip() {
        // Setup minimal safe main
        given(safeMainContract.loadSelectedSafe()).willReturn(Single.error(NotImplementedError()))
        given(safeMainContract.observeSafes()).willReturn(Flowable.error(NotImplementedError()))
        // We don't want to emit a value
        given(fingerprintSetupContract.observeFingerprintForSetup()).willReturn(PublishSubject.create())
        val activity = activityRule.launchActivity(null)
        then(encryptionManagerMock).should(times(1)).unlocked()

        onView(withId(R.id.layout_fingerprint_setup_continue)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_fingerprint_setup_continue)).perform(ViewActions.click())

        then(encryptionManagerMock).should(times(2)).unlocked()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        // Contract interaction
        then(fingerprintSetupContract).should().observeFingerprintForSetup()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.FINGERPRINT))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.SAFE_MAIN))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.NO_SAFES))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.FINGERPRINT)
        // We started 2 activities that call this method
        then(eventTrackerMock).should(times(2)).setCurrentScreenId(UIMockUtils.any(), UIMockUtils.any())
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(FingerprintSetupActivity::class.java.name))
        Intents.intended(matchesIntentExactly(SafeMainActivity.createIntent(activity).clearStack()))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun observeError() {
        // Setup minimal safe main
        given(safeMainContract.loadSelectedSafe()).willReturn(Single.error(NotImplementedError()))
        given(safeMainContract.observeSafes()).willReturn(Flowable.error(NotImplementedError()))
        // We don't want to emit a value
        val fingerprintSubject = PublishSubject.create<Boolean>()
        given(fingerprintSetupContract.observeFingerprintForSetup()).willReturn(fingerprintSubject)
        val activity = activityRule.launchActivity(null)
        then(encryptionManagerMock).should(times(1)).unlocked()

        // Fingerprint not recognized
        fingerprintSubject.onError(IllegalArgumentException())

        onView(withText(activity.getString(R.string.unknown_error))).inRoot(withDecorView(not(activity.window.decorView))).check(matches(isDisplayed()))

        then(encryptionManagerMock).should(times(2)).unlocked()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        // Contract interaction
        then(fingerprintSetupContract).should().observeFingerprintForSetup()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.FINGERPRINT))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.SAFE_MAIN))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.NO_SAFES))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.FINGERPRINT)
        // We started 2 activities that call this method
        then(eventTrackerMock).should(times(2)).setCurrentScreenId(UIMockUtils.any(), UIMockUtils.any())
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(FingerprintSetupActivity::class.java.name))
        Intents.intended(matchesIntentExactly(SafeMainActivity.createIntent(activity).clearStack()))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun observe() {
        // Setup minimal safe main
        given(safeMainContract.loadSelectedSafe()).willReturn(Single.error(NotImplementedError()))
        given(safeMainContract.observeSafes()).willReturn(Flowable.error(NotImplementedError()))
        // We don't want to emit a value
        val fingerprintSubject = PublishSubject.create<Boolean>()
        given(fingerprintSetupContract.observeFingerprintForSetup()).willReturn(fingerprintSubject)
        val activity = activityRule.launchActivity(null)
        then(encryptionManagerMock).should(times(1)).unlocked()

        onView(withId(R.id.layout_fingerprint_setup_continue)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_fingerprint_setup_info_title)).check(matches(allOf(isDisplayed(), withText(R.string.setup_fingerprint_place))))
        onView(withId(R.id.layout_fingerprint_setup_continue_label)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.skip))))

        // Fingerprint not recognized
        fingerprintSubject.onNext(false)

        onView(withId(R.id.layout_fingerprint_setup_continue)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_fingerprint_setup_info_title)).check(matches(allOf(isDisplayed(), withText(R.string.setup_fingerprint_place))))
        onView(withId(R.id.layout_fingerprint_setup_continue_label)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.skip))))
        // Wait for snackbar
        Thread.sleep(300)
        // Check and dismiss snackbar
        onView(allOf(isDisplayed(), withText(context.getString(R.string.fingerprint_not_recognized)))).perform(ViewActions.swipeRight())
        // Wait for snackbar to disappear
        Thread.sleep(200)

        // Fingerprint setup
        fingerprintSubject.onNext(true)

        onView(withId(R.id.layout_fingerprint_setup_continue)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_fingerprint_setup_info_title)).check(matches(allOf(isDisplayed(), withText(R.string.fingerprint_confirmed))))
        onView(withId(R.id.layout_fingerprint_setup_continue_label)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.finish))))

        onView(withId(R.id.layout_fingerprint_setup_continue)).perform(ViewActions.click())

        then(encryptionManagerMock).should(times(2)).unlocked()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        // Contract interaction
        then(fingerprintSetupContract).should().observeFingerprintForSetup()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.FINGERPRINT))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.SAFE_MAIN))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.NO_SAFES))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.FINGERPRINT)
        // We started 2 activities that call this method
        then(eventTrackerMock).should(times(2)).setCurrentScreenId(UIMockUtils.any(), UIMockUtils.any())
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(FingerprintSetupActivity::class.java.name))
        Intents.intended(matchesIntentExactly(SafeMainActivity.createIntent(activity).clearStack()))
        Intents.assertNoUnverifiedIntents()
    }
}
