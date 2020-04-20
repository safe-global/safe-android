package io.gnosis.safe.ui

import androidx.annotation.NavigationRes
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseRobot {

    @Before
    private fun setup() {
        val navController = TestNavHostController(
            ApplicationProvider.getApplicationContext()
        )
        navController.setGraph(navigationGraphId)
    }

    fun fillEditText(resId: Int, text: String): ViewInteraction =
        onView(withId(resId)).perform(ViewActions.replaceText(text), ViewActions.closeSoftKeyboard())

    fun clickButton(resId: Int): ViewInteraction = onView((withId(resId))).perform(ViewActions.click())

    fun textView(resId: Int): ViewInteraction = onView(withId(resId))

    fun matchText(viewInteraction: ViewInteraction, text: String): ViewInteraction =
        viewInteraction.check(ViewAssertions.matches(ViewMatchers.withText(text)))

    fun matchText(resId: Int, text: String): ViewInteraction =
        matchText(textView(resId), text)

    fun clickListItem(listRes: Int, position: Int): ViewInteraction =
        onData(anything())
            .inAdapterView(allOf(withId(listRes)))
            .atPosition(position)
            .perform(ViewActions.click())


    @get:NavigationRes
    abstract val navigationGraphId: Int

}

