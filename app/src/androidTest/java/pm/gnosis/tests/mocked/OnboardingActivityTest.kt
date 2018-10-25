package pm.gnosis.tests.mocked

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.assertion.ViewAssertions.*
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers
import android.support.test.espresso.intent.matcher.IntentMatchers.*
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import io.reactivex.Flowable
import io.reactivex.Single
import org.hamcrest.Matchers
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.BDDMockito.*
import org.mockito.Mockito.mock
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.onboarding.OnboardingIntroActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.splash.StartMain
import pm.gnosis.tests.BaseUiTest
import pm.gnosis.tests.utils.UIMockUtils
import pm.gnosis.tests.utils.newPasswordContractMock

@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest: BaseUiTest() {
    @JvmField
    @Rule
    val activityRule = ActivityTestRule(OnboardingIntroActivity::class.java, true, false)

    // ViewModel mocks

    private val passwordSetupContract = newPasswordContractMock()

    @Before
    fun setup() {
        Intents.init()
        val comp = setupBaseInjects()
        given(comp.toolbarHelper()).willReturn(ToolbarHelper())
        given(comp.viewModelFactory()).willReturn(object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    PasswordSetupContract::class.java -> passwordSetupContract
                    else -> null
                } as T
            }
        })
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    private fun checkBottomSheet(visible: Boolean) {
        val matcher = if (visible) matches(isDisplayed()) else doesNotExist()
        onView(withId(R.id.bottom_sheet_terms_and_conditions_title)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_description)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_first_bullet)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_first_bullet_text)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_second_bullet)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_second_bullet_text)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_reject)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_agree)).check(matcher)
    }

    @Test
    fun startOnboardingIntroInitialState() {
        val activity = activityRule.launchActivity(null)

        onView(withId(R.id.layout_onboarding_intro_logo)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_onboarding_intro_get_started)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_onboarding_intro_info)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.onboarding_intro_info))))
        onView(withText(R.string.personal_edition)).check(matches(isCompletelyDisplayed()))
        checkBottomSheet(false)

        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.WELCOME)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(OnboardingIntroActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun startOnboardingIntroDeclineTerms() {
        val activity = activityRule.launchActivity(null)

        onView(withId(R.id.layout_onboarding_intro_get_started)).check(matches(isCompletelyDisplayed()))
        checkBottomSheet(false)
        onView(withId(R.id.layout_onboarding_intro_get_started)).perform(click())
        checkBottomSheet(true)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_reject)).perform(click())
        checkBottomSheet(false)

        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME_TERMS))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.WELCOME)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(OnboardingIntroActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun startOnboardingIntroAcceptsTerms() {
        val activity = activityRule.launchActivity(null)

        onView(withId(R.id.layout_onboarding_intro_get_started)).check(matches(isCompletelyDisplayed()))
        checkBottomSheet(false)
        onView(withId(R.id.layout_onboarding_intro_get_started)).perform(click())
        checkBottomSheet(true)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_agree)).perform(click())
        checkBottomSheet(false)

        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME_TERMS))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.WELCOME)
        // We started the password setup screen
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD))
        // We started 2 activities that call this method
        then(eventTrackerMock).should(times(2)).setCurrentScreenId(UIMockUtils.any(), UIMockUtils.any())
        then(eventTrackerMock).shouldHaveNoMoreInteractions()

        Intents.intended(hasComponent(OnboardingIntroActivity::class.java.name))
        Intents.intended(hasComponent(PasswordSetupActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()
    }
}
