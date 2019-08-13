package pm.gnosis.heimdall.helpers

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
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

        fun getValidationInfo(context: Context): String {
            val conditions = listOf(
                PasswordValidationCondition.MinimumCharacters(true),
                PasswordValidationCondition.OneNumberOneLetter(true),
                PasswordValidationCondition.NonIdenticalCharacters(true)
            )
            val message = StringBuilder()
            conditions.forEachIndexed { index, condition ->
                message.append(context.getString(condition.messageRes) + if (index < conditions.size - 1) "\n" else "")
            }
            return message.toString()
        }

        fun validate(password: String): Collection<PasswordValidationCondition> =
            listOf(
                PasswordValidationCondition.MinimumCharacters(password.length >= MIN_CHARS),
                PasswordValidationCondition.OneNumberOneLetter(
                    password.matches(CONTAINS_DIGIT_REGEX) && password.matches(
                        CONTAINS_LETTER_REGEX
                    )
                ),
                PasswordValidationCondition.NonIdenticalCharacters(!password.hasConsecutiveChars(CONSECUTIVE_CHARS) && password.isNotEmpty())
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
            conditions.forEachIndexed { index, condition ->
                validPassword = validPassword && condition.valid
                val color = when {
                    emptyPassword -> context.getColorCompat(R.color.dark_grey)
                    condition.valid -> context.getColorCompat(R.color.green)
                    else -> context.getColorCompat(R.color.tomato)
                }
                message.appendText(
                    context.getString(condition.messageRes) + if (index < conditions.size - 1) "\n" else "",
                    ForegroundColorSpan(color)
                )
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


        fun applyToView(
            passwordInput: EditText, passwordMessage: TextView, message: String?, validPassword: Boolean
        ) {

            passwordInput.setCompoundDrawables(
                right =
                if (passwordInput.text.isEmpty()) null
                else ContextCompat.getDrawable(passwordInput.context, if (validPassword) R.drawable.ic_green_check else R.drawable.ic_error)
            )

            passwordMessage.text = message
        }

        fun resetView( passwordInput: EditText, passwordMessage: TextView) {
            passwordInput.setCompoundDrawables(
                right = null
            )
            passwordMessage.text = null
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
