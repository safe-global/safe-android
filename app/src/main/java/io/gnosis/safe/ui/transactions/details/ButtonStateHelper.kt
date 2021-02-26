package io.gnosis.safe.ui.transactions.details

class ButtonStateHelper(
    val hasBeenRejected: Boolean,
    val needsYourConfirmation: Boolean,
    val isRejection: Boolean,
    val needsExecution: Boolean,
    val canReject: Boolean,
    val isOwner: Boolean,
    val completed: Boolean
) {

    fun rejectButtonIsVisible(): Boolean =
        when {
            !isOwner || completed -> {
                false
            }
            isRejection || !canReject -> {
                false
            }
            !isRejection && !needsYourConfirmation && hasBeenRejected && needsExecution -> {
                false
            }
            else -> true

        }

    fun confirmButtonIsVisible(): Boolean =
        when {
            !isOwner || completed -> {
                false
            }
            needsYourConfirmation -> {
                true
            }
            !isRejection && !hasBeenRejected && !needsExecution -> {
                true
            }
            !isRejection && !needsYourConfirmation && hasBeenRejected && needsExecution -> {
                true
            }
            else -> false
        }

    fun rejectButtonIsEnabled(): Boolean =
        when {
            !rejectButtonIsVisible() -> {
                false
            }
            !isRejection && needsYourConfirmation && hasBeenRejected && !needsExecution -> {
                false
            }
            else -> true
        }

    fun confirmButtonIsEnabled(): Boolean = when {
        !confirmButtonIsVisible() -> {
            false
        }
        !needsYourConfirmation && !needsExecution -> {
            false
        }
        else -> true
    }

    fun spacerIsVisible(): Boolean = confirmButtonIsVisible() && rejectButtonIsVisible()

    fun buttonContainerIsVisible(): Boolean = confirmButtonIsVisible() || rejectButtonIsVisible()

}
