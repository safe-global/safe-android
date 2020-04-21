package io.gnosis.safe.ui

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything

abstract class BaseRobot {

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

}

