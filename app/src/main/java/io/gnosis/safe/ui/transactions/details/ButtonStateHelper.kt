package io.gnosis.safe.ui.transactions.details

class ButtonStateHelper(
    val hasBeenRejected: Boolean,
    val needsYourConfirmation: Boolean,
    val isRejection: Boolean,
    val needsExecution: Boolean
) {

    fun rejectButtonIsVisible(): Boolean {
        if (isRejection) {
            return false
        }
        if (!isRejection && !needsYourConfirmation && hasBeenRejected && needsExecution) {
            return false
        }
        return true
    }

    fun confirmButtonIsVisible(): Boolean {
        if (needsYourConfirmation) {
            return true
        }
        if (!isRejection && !hasBeenRejected && !needsExecution) {
            return true
        }
        if (!isRejection && !needsYourConfirmation && hasBeenRejected && needsExecution) {
            return true
        }
        return false
    }

    fun rejectButtonIsEnabled(): Boolean {
        if (!rejectButtonIsVisible()) {
            return false
        }
        return true
    }

    fun confirmButtonIsEnabled(): Boolean {
        if (!confirmButtonIsVisible()) {
            return false
        }
        if (!needsYourConfirmation && !needsExecution) {
            return false
        }
        return true
    }

    fun spacerIsVisible(): Boolean = confirmButtonIsVisible() && rejectButtonIsVisible()

    fun buttonContainerIsVisible(): Boolean = confirmButtonIsVisible() || rejectButtonIsVisible()

}
