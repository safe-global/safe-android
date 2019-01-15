package pm.gnosis.tests.mocked

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import io.reactivex.Single
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.PasswordHelper
import pm.gnosis.heimdall.helpers.PasswordValidationCondition
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.onboarding.password.PasswordConfirmActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupActivity
import pm.gnosis.heimdall.ui.onboarding.password.PasswordSetupContract
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.tests.BaseUiTest
import pm.gnosis.tests.utils.UIMockUtils
import pm.gnosis.tests.utils.matchesIntentExactly
import pm.gnosis.utils.hexToByteArray

@RunWith(AndroidJUnit4::class)
class PasswordSetupActivityTest : BaseUiTest() {
    @JvmField
    @Rule
    val activityRule = ActivityTestRule(PasswordSetupActivity::class.java, true, false)

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

    @Test
    fun initialState() {
        val activity = activityRule.launchActivity(null)

        // Bottom Bar
        onView(withId(R.id.layout_password_setup_next)).check(matches(allOf(isCompletelyDisplayed(), not(isEnabled()))))
        onView(withId(R.id.layout_password_setup_bottom_container)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_password_setup_next_arrow)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_password_setup_next_text)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.next))))

        // Title
        onView(withId(R.id.layout_password_setup_title)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.create_password))))
        // Scrollable content
        onView(withText(R.string.setup_password_info)).check(matches(isDisplayed()))
        onView(withId(R.id.layout_password_setup_password)).check(matches(isDisplayed()))
        onView(withId(R.id.layout_password_setup_validation_info)).check(matches(allOf(isDisplayed(), withText(""))))

        // Wait for initial input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_setup_validation_info)).check(matches(allOf(isDisplayed(), withText(""))))

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).should().validatePassword(UIMockUtils.any())
        then(passwordSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.intended(hasComponent(PasswordSetupActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()
    }

    private fun testConditions(context: Context, emptyPassword: Boolean, conditions: List<PasswordValidationCondition>) {
        given(passwordSetupContract.validatePassword(UIMockUtils.any())).willReturn(Single.just(DataResult(conditions)))

        // Trigger check
        onView(withId(R.id.layout_password_setup_password)).perform(typeText("ab"))
        // Wait for input delay
        Thread.sleep(600)
        val checkedResult = PasswordHelper.Handler.handleConditions(context, emptyPassword, conditions)
        onView(withId(R.id.layout_password_setup_validation_info)).check(matches(allOf(isDisplayed(), withText(checkedResult.message.toString()))))
        val nextEnabledMatcher = if (checkedResult.validPassword) isEnabled() else not(isEnabled())
        onView(withId(R.id.layout_password_setup_next)).check(matches(allOf(isDisplayed(), nextEnabledMatcher)))
    }

    @Test
    fun checkPasswords() {
        given(passwordSetupContract.validatePassword(UIMockUtils.any())).willReturn(Single.just(ErrorResult(IllegalStateException())))
        val activity = activityRule.launchActivity(null)
        Intents.intended(hasComponent(PasswordSetupActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        onView(withId(R.id.layout_password_setup_next)).check(matches(allOf(isCompletelyDisplayed(), not(isEnabled()))))
        onView(withId(R.id.layout_password_setup_validation_info)).check(matches(allOf(isDisplayed(), withText(""))))
        // Wait for initial input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_setup_validation_info)).check(matches(allOf(isDisplayed(), withText(""))))

        testConditions(activity, true, listOf(PasswordValidationCondition.MinimumCharacters(false)))

        testConditions(
            activity, false, listOf(
                PasswordValidationCondition.MinimumCharacters(false),
                PasswordValidationCondition.NonIdenticalCharacters(false),
                PasswordValidationCondition.OneNumberOneLetter(false)
            )
        )

        testConditions(
            activity, false, listOf(
                PasswordValidationCondition.MinimumCharacters(false),
                PasswordValidationCondition.NonIdenticalCharacters(true),
                PasswordValidationCondition.OneNumberOneLetter(false)
            )
        )

        testConditions(
            activity, false, listOf(
                PasswordValidationCondition.MinimumCharacters(true),
                PasswordValidationCondition.NonIdenticalCharacters(true)
            )
        )

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).should(times(5)).validatePassword(UIMockUtils.any())
        then(passwordSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun startConfirmPasswords() {
        given(passwordSetupContract.validatePassword(UIMockUtils.any())).willReturn(Single.just(DataResult(emptyList())))
        val activity = activityRule.launchActivity(null)
        Intents.intended(hasComponent(PasswordSetupActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        // Wait for input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_setup_next)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_password_setup_validation_info)).check(matches(allOf(isDisplayed(), withText(""))))

        given(passwordSetupContract.passwordToHash(UIMockUtils.any())).willReturn(Single.just(ErrorResult(IllegalStateException())))
        onView(withId(R.id.layout_password_setup_next)).perform(click())
        Intents.assertNoUnverifiedIntents()

        given(passwordSetupContract.passwordToHash(UIMockUtils.any())).willReturn(Single.just(DataResult("01020304".hexToByteArray())))
        onView(withId(R.id.layout_password_setup_next)).perform(click())
        Intents.intended(matchesIntentExactly(PasswordConfirmActivity.createIntent(activity, "01020304".hexToByteArray())))
        Intents.assertNoUnverifiedIntents()

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).should(times(3)).validatePassword("")
        then(passwordSetupContract).should(times(2)).passwordToHash("")
        then(passwordSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD)
        // We started the confirm password screen
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))
        // We started 2 activities that call this method
        then(eventTrackerMock).should(times(2)).setCurrentScreenId(UIMockUtils.any(), UIMockUtils.any())
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }

    //@Test
    fun startConfirmPasswordAndBack() {
        given(passwordSetupContract.validatePassword(UIMockUtils.any())).willReturn(Single.just(DataResult(emptyList())))
        val activity = activityRule.launchActivity(null)
        Intents.intended(hasComponent(PasswordSetupActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD))

        // Wait for input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_setup_next)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_password_setup_validation_info)).check(matches(allOf(isDisplayed(), withText(""))))

        given(passwordSetupContract.passwordToHash(UIMockUtils.any())).willReturn(Single.just(DataResult("01020304".hexToByteArray())))
        onView(withId(R.id.layout_password_setup_next)).perform(click())
        Intents.intended(matchesIntentExactly(PasswordConfirmActivity.createIntent(activity, "01020304".hexToByteArray())))
        Intents.assertNoUnverifiedIntents()

        // We started the confirm password screen
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))

        onView(withId(R.id.layout_password_confirm_text)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.confirm))))
        onView(withId(R.id.layout_password_setup_title)).check(doesNotExist())
        onView(withId(R.id.layout_password_confirm_back)).perform(click())

        onView(withId(R.id.layout_password_setup_title)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.create_password))))
        onView(withId(R.id.layout_password_confirm_text)).check(doesNotExist())

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        // We use at least 2 since we return from the other activity and it depends on the emulator how fast onStart is called
        then(passwordSetupContract).should(atLeast(2)).validatePassword("")
        then(passwordSetupContract).should(times(1)).passwordToHash("")
        // Check tracking
        // We use at least 1 since we return from the other activity and it depends on the emulator how fast onStart is called
        then(eventTrackerMock).should(atLeast(1)).submit(Event.ScreenView(ScreenId.PASSWORD))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD)
        // We started 2 activities that call this method
        then(eventTrackerMock).should(times(2)).setCurrentScreenId(UIMockUtils.any(), UIMockUtils.any())
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }
}
