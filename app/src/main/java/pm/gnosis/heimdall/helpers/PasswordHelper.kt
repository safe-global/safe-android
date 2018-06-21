package pm.gnosis.heimdall.helpers

import android.content.Context
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.TextView
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.utils.setCompoundDrawables
import pm.gnosis.svalinn.common.utils.appendText
import pm.gnosis.svalinn.common.utils.getColorCompat


object PasswordHelper {
    object Validator {
        private const val MIN_CHARS = 8
        private const val CONSECUTIVE_CHARS = 3
        private val CONTAINS_DIGIT_REGEX = ".*\\d+.*".toRegex()
        private val CONTAINS_LETTER_REGEX = ".*\\p{L}.*".toRegex()

        fun validate(password: String): Collection<PasswordValidationCondition> =
            listOf(
                PasswordValidationCondition.NonIdenticalCharacters(!password.hasConsecutiveChars(CONSECUTIVE_CHARS) && password.isNotEmpty()),
                PasswordValidationCondition.MinimumCharacters(password.length >= MIN_CHARS),
                PasswordValidationCondition.OneNumberOneLetter(
                    password.matches(CONTAINS_DIGIT_REGEX) && password.matches(
                        CONTAINS_LETTER_REGEX
                    )
                )
            )

        private fun String.hasConsecutiveChars(consecutiveChars: Int): Boolean {
            var currentStart = 0
            this.forEachIndexed { index, c ->
                if (c != this[currentStart]) currentStart = index
                if ((index - currentStart + 1) >= consecutiveChars) return true
            }
            return false
        }
    }

    object Handler {
        fun handleConditions(context: Context, emptyPassword: Boolean, conditions: Collection<PasswordValidationCondition>): Result {
            var validPassword = true
            val message = SpannableStringBuilder()
            conditions.forEach {
                validPassword = validPassword && it.valid
                val color = when {
                    emptyPassword -> context.getColorCompat(R.color.battleship_grey)
                    it.valid -> context.getColorCompat(R.color.green_teal)
                    else -> context.getColorCompat(R.color.tomato)
                }
                message.appendText(context.getString(it.messageRes), ForegroundColorSpan(color))
            }
            return Result(message, validPassword)
        }

        fun applyToView(
            passwordInput: EditText, passwordMessage: TextView, conditions: Collection<PasswordValidationCondition>
        ): Result =
            handleConditions(passwordInput.context, passwordInput.text.isEmpty(), conditions).apply {
                passwordInput.setCompoundDrawables(
                    right =
                    if (passwordInput.text.isEmpty()) null
                    else ContextCompat.getDrawable(passwordInput.context, if (validPassword) R.drawable.ic_green_check else R.drawable.ic_error)
                )
                passwordMessage.text = message
            }


        data class Result(val message: CharSequence, val validPassword: Boolean)
    }
}

sealed class PasswordValidationCondition(val valid: Boolean, @StringRes val messageRes: Int) {
    data class NonIdenticalCharacters(val hasNonIdenticalCharacters: Boolean) :
        PasswordValidationCondition(hasNonIdenticalCharacters, R.string.password_validation_identical_characters)

    data class MinimumCharacters(val hasEnoughCharacters: Boolean) :
        PasswordValidationCondition(hasEnoughCharacters, R.string.password_validation_minimum_characters)

    data class OneNumberOneLetter(val hasAtLeastOneNumberAndOneLetter: Boolean) :
        PasswordValidationCondition(hasAtLeastOneNumberAndOneLetter, R.string.password_validation_one_number_one_letter)
}
