package pm.gnosis.heimdall.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.SpannableStringBuilder
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.tests.utils.mockGetColor
import pm.gnosis.tests.utils.mockGetString

@RunWith(MockitoJUnitRunner::class)
class PasswordHelperTest {

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var passwordInput: EditText

    @Mock
    lateinit var passwordMessage: TextView

    private fun testPassword(password: String, expectedConditions: List<PasswordValidationCondition>) {
        val validPassword = expectedConditions.all { it.valid }
        context.mockGetColor()
        context.mockGetString()
        val statusDrawable = mock(Drawable::class.java)
        given(passwordInput.context).willReturn(context)
        given(passwordInput.context.resources.getDrawable(anyInt())).willReturn(statusDrawable)
        val editable = mock(Editable::class.java)
        given(editable.length).willReturn(password.length)
        given(passwordInput.text).willReturn(editable)

        val conditions = PasswordHelper.Validator.validate(password)
        assertEquals(expectedConditions, conditions)

        val result = PasswordHelper.Handler.applyToView(passwordInput, passwordMessage, conditions)
        assertEquals(validPassword, result.validPassword)

        val colorCounts = mutableMapOf<Int, Int>()
        expectedConditions.forEach {
            then(context).should().getString(it.messageRes)
            val color = when {
                password.isEmpty() -> R.color.battleship_grey
                it.valid -> R.color.green_teal
                else -> R.color.tomato
            }
            colorCounts[color] = (colorCounts[color] ?: 0) + 1
        }
        colorCounts.forEach {
            then(context.resources).should(times(it.value)).getColor(it.key)
        }
        then(context).should(atLeastOnce()).resources
        then(context).shouldHaveNoMoreInteractions()

        if (password.isEmpty()) {
            then(passwordInput).should().setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        } else {
            then(passwordInput).should().setCompoundDrawablesWithIntrinsicBounds(null, null, statusDrawable, null)
        }
        then(passwordInput).should(atLeastOnce()).context
        then(passwordInput).should(atLeastOnce()).text
        then(passwordInput).shouldHaveNoMoreInteractions()

        then(passwordMessage).should().text = any(SpannableStringBuilder::class.java)
        then(passwordInput).shouldHaveNoMoreInteractions()
    }

    @Test
    fun emptyPassword() {
        val password = ""
        val expectedConditions = listOf(
            PasswordValidationCondition.NonIdenticalCharacters(false),
            PasswordValidationCondition.MinimumCharacters(false),
            PasswordValidationCondition.OneNumberOneLetter(false)
        )
        testPassword(password, expectedConditions)
    }

    @Test
    fun identicalCharacters() {
        val password = "aaabbb111"
        val expectedConditions = listOf(
            PasswordValidationCondition.NonIdenticalCharacters(false),
            PasswordValidationCondition.MinimumCharacters(true),
            PasswordValidationCondition.OneNumberOneLetter(true)
        )
        testPassword(password, expectedConditions)
    }

    @Test
    fun minimumCharacters() {
        val password = "acbb1"
        val expectedConditions = listOf(
            PasswordValidationCondition.NonIdenticalCharacters(true),
            PasswordValidationCondition.MinimumCharacters(false),
            PasswordValidationCondition.OneNumberOneLetter(true)
        )
        testPassword(password, expectedConditions)
    }

    @Test
    fun oneNumberOneLetter() {
        val password = "a1"
        val expectedConditions = listOf(
            PasswordValidationCondition.NonIdenticalCharacters(true),
            PasswordValidationCondition.MinimumCharacters(false),
            PasswordValidationCondition.OneNumberOneLetter(true)
        )
        testPassword(password, expectedConditions)
    }

    @Test
    fun validPassword() {
        val password = "qweasd123"
        val expectedConditions = listOf(
            PasswordValidationCondition.NonIdenticalCharacters(true),
            PasswordValidationCondition.MinimumCharacters(true),
            PasswordValidationCondition.OneNumberOneLetter(true)
        )
        testPassword(password, expectedConditions)
    }
}
