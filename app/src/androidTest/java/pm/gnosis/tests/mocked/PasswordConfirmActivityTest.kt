package pm.gnosis.tests.mocked

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import io.reactivex.Single
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.helpers.ToolbarHelper
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.onboarding.password.*
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.tests.BaseUiTest
import pm.gnosis.tests.utils.UIMockUtils
import pm.gnosis.tests.utils.matchesIntentExactly
import pm.gnosis.utils.hexToByteArray

@RunWith(AndroidJUnit4::class)
class PasswordConfirmActivityTest : BaseUiTest() {
    @JvmField
    @Rule
    val activityRule = ActivityTestRule(PasswordConfirmActivity::class.java, true, false)

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
        val activity = activityRule.launchActivity(PasswordConfirmActivity.createIntent(context, "01020304".hexToByteArray()))
        Intents.intended(hasComponent(PasswordConfirmActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        // Bottom Bar
        onView(withId(R.id.layout_password_confirm_confirm)).check(matches(allOf(isCompletelyDisplayed(), not(isEnabled()))))
        onView(withId(R.id.layout_password_confirm_bottom_container)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_password_confirm_back)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_password_confirm_next_arrow)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.layout_password_confirm_text)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.confirm))))

        // Title
        onView(withId(R.id.layout_password_confirm_title)).check(matches(allOf(isCompletelyDisplayed(), withText(R.string.confirm_password))))
        // Scrollable content
        onView(withText(R.string.confirm_password_info)).check(matches(isDisplayed()))
        onView(withId(R.id.layout_password_confirm_password)).check(matches(isDisplayed()))
        onView(withId(R.id.layout_password_confirm_info)).check(matches(not(isDisplayed())))
        onView(withId(R.id.layout_password_confirm_progress)).check(matches(not(isDisplayed())))

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).shouldHaveZeroInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD_CONFIRM)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun backPressed() {
        val activity = activityRule.launchActivity(PasswordConfirmActivity.createIntent(context, "01020304".hexToByteArray()))
        Intents.intended(hasComponent(PasswordConfirmActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        onView(withId(R.id.layout_password_confirm_back)).perform(ViewActions.click())
        assertTrue("Activity should finish", activity.isFinishing)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).shouldHaveZeroInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD_CONFIRM)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun passwordChanges() {
        val passwordHash = "01020304".hexToByteArray()
        val activity = activityRule.launchActivity(PasswordConfirmActivity.createIntent(context, passwordHash))
        Intents.intended(hasComponent(PasswordConfirmActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        given(passwordSetupContract.isSamePassword(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(ErrorResult(IllegalStateException())))

        onView(withId(R.id.layout_password_confirm_password)).perform(ViewActions.typeText("Hel"))
        // Wait for input delay
        Thread.sleep(600)
        // Error: nothing changes
        onView(withId(R.id.layout_password_confirm_confirm)).check(matches(allOf(isCompletelyDisplayed(), not(isEnabled()))))
        onView(withId(R.id.layout_password_confirm_info)).check(matches(not(isDisplayed())))


        given(passwordSetupContract.isSamePassword(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(DataResult(false)))
        onView(withId(R.id.layout_password_confirm_password)).perform(ViewActions.typeText("lo F"))
        // Wait for input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_confirm_confirm)).check(matches(allOf(isCompletelyDisplayed(), not(isEnabled()))))
        onView(withId(R.id.layout_password_confirm_info)).check(matches(isDisplayed()))


        given(passwordSetupContract.isSamePassword(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(DataResult(true)))
        onView(withId(R.id.layout_password_confirm_password)).perform(ViewActions.typeText("red"))
        // Wait for input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_confirm_confirm)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))
        onView(withId(R.id.layout_password_confirm_info)).check(matches(not(isDisplayed())))

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).should().isSamePassword(passwordHash, "Hel")
        then(passwordSetupContract).should().isSamePassword(passwordHash, "Hello F")
        then(passwordSetupContract).should().isSamePassword(passwordHash, "Hello Fred")
        then(passwordSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD_CONFIRM)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun submitPasswordClicksTooShort() {
        val passwordHash = "01020304".hexToByteArray()
        val activity = activityRule.launchActivity(PasswordConfirmActivity.createIntent(context, passwordHash))
        Intents.intended(hasComponent(PasswordConfirmActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        // Enable next button
        given(passwordSetupContract.isSamePassword(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(DataResult(true)))
        onView(withId(R.id.layout_password_confirm_password)).perform(ViewActions.typeText("Modest Mouse - Float On"))
        // Wait for input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_confirm_confirm)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))

        // Test password too short
        given(passwordSetupContract.createAccount(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(ErrorResult(PasswordInvalidException(PasswordNotLongEnough(5, 6)))))

        onView(withId(R.id.layout_password_confirm_confirm)).perform(ViewActions.click())
        // Error: show snackbar
        onView(withText(context.getString(R.string.password_too_short))).check(matches(isDisplayed()))

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).should().isSamePassword(passwordHash, "Modest Mouse - Float On")
        then(passwordSetupContract).should().createAccount(passwordHash, "Modest Mouse - Float On")
        then(passwordSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD_CONFIRM)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun submitPasswordClicksDoesntMatch() {
        val passwordHash = "01020304".hexToByteArray()
        val activity = activityRule.launchActivity(PasswordConfirmActivity.createIntent(context, passwordHash))
        Intents.intended(hasComponent(PasswordConfirmActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        // Enable next button
        given(passwordSetupContract.isSamePassword(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(DataResult(true)))
        onView(withId(R.id.layout_password_confirm_password)).perform(ViewActions.typeText("Modest Mouse - Float On"))
        // Wait for input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_confirm_confirm)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))

        // Test password doesn't match
        given(passwordSetupContract.createAccount(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(ErrorResult(PasswordInvalidException(PasswordsNotEqual))))

        onView(withId(R.id.layout_password_confirm_confirm)).perform(ViewActions.click())
        // Error: show snackbar
        onView(withText(context.getString(R.string.password_doesnt_match))).check(matches(isDisplayed()))

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).should().isSamePassword(passwordHash, "Modest Mouse - Float On")
        then(passwordSetupContract).should().createAccount(passwordHash, "Modest Mouse - Float On")
        then(passwordSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD_CONFIRM)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }

    @Test
    fun submitPasswordClicksValid() {
        val passwordHash = "01020304".hexToByteArray()
        val activity = activityRule.launchActivity(PasswordConfirmActivity.createIntent(context, passwordHash))
        Intents.intended(hasComponent(PasswordConfirmActivity::class.java.name))
        Intents.assertNoUnverifiedIntents()

        // Enable next button
        given(passwordSetupContract.isSamePassword(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(DataResult(true)))
        onView(withId(R.id.layout_password_confirm_password)).perform(ViewActions.typeText("Modest Mouse - Float On"))
        // Wait for input delay
        Thread.sleep(600)
        onView(withId(R.id.layout_password_confirm_confirm)).check(matches(allOf(isCompletelyDisplayed(), isEnabled())))

        val intent = Intent("Riders on the storm")
        given(passwordSetupContract.createAccount(UIMockUtils.any(), UIMockUtils.any()))
            .willReturn(Single.just(DataResult(intent)))

        onView(withId(R.id.layout_password_confirm_confirm)).perform(ViewActions.click())
        Intents.intended(matchesIntentExactly(intent))

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        // Contract interaction
        then(passwordSetupContract).should().isSamePassword(passwordHash, "Modest Mouse - Float On")
        then(passwordSetupContract).should().createAccount(passwordHash, "Modest Mouse - Float On")
        then(passwordSetupContract).shouldHaveNoMoreInteractions()
        // Check tracking
        then(eventTrackerMock).should().submit(Event.ScreenView(ScreenId.PASSWORD_CONFIRM))
        then(eventTrackerMock).should().setCurrentScreenId(activity, ScreenId.PASSWORD_CONFIRM)
        then(eventTrackerMock).shouldHaveNoMoreInteractions()
        Intents.assertNoUnverifiedIntents()
    }
}
