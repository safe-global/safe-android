package pm.gnosis.tests.mocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.onboarding.OnboardingIntroActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.tests.BaseUiTest
import pm.gnosis.tests.utils.UIMockUtils

@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest: BaseUiTest() {
    @JvmField
    @Rule
    val activityRule = ActivityTestRule(OnboardingIntroActivity::class.java, true, false)

    // ViewModel mocks

    private val passwordSetupContract = mock(PasswordSetupContract::class.java)

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
        onView(withId(R.id.bottom_sheet_terms_and_conditions_first_bullet)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_first_bullet_text)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_second_bullet)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_second_bullet_text)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_third_bullet)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_third_bullet_text)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_reject)).check(matcher)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_agree)).check(matcher)
    }

    @Test
    fun initialState() {
        val activity = activityRule.launchActivity(null)

        onView(withId(R.id.layout_onboarding_intro_logo)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_onboarding_intro_get_started)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_onboarding_intro_info)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.onboarding_intro_info))))
        checkBottomSheet(false)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.WELCOME)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(OnboardingIntroActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun declineTerms() {
        val activity = activityRule.launchActivity(null)

        onView(withId(R.id.layout_onboarding_intro_get_started)).check(matches(isCompletelyDisplayed()))
        checkBottomSheet(false)
        onView(withId(R.id.layout_onboarding_intro_get_started)).perform(click())
        checkBottomSheet(true)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_reject)).perform(click())
        checkBottomSheet(false)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME))
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.WELCOME_TERMS))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.WELCOME)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(OnboardingIntroActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun acceptsTerms() {
        val activity = activityRule.launchActivity(null)

        onView(withId(R.id.layout_onboarding_intro_get_started)).check(matches(isCompletelyDisplayed()))
        checkBottomSheet(false)
        onView(withId(R.id.layout_onboarding_intro_get_started)).perform(click())
        checkBottomSheet(true)
        onView(withId(R.id.bottom_sheet_terms_and_conditions_agree)).perform(click())
        checkBottomSheet(false)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
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
