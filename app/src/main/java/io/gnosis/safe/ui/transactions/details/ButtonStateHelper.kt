package io.gnosis.safe.ui.transactions.details

class ButtonStateHelper(
    val canSign: Boolean,
    val canExecute: Boolean,
    val hasOwnerKey: Boolean,
    val hasBeenRejected: Boolean,
    val isRejection: Boolean,
    val awaitingConfirmations: Boolean,
    val awaitingExecution: Boolean,
    val nextInLine: Boolean
) {
    fun confirmationButtonIsVisible(): Boolean =
        when {
            isRejection && awaitingConfirmations && canSign -> true
            isRejection && awaitingConfirmations && hasOwnerKey && !canSign -> true
            isRejection && awaitingExecution && canExecute && nextInLine -> true

            !isRejection && awaitingConfirmations && canSign -> true
            !isRejection && awaitingConfirmations && hasOwnerKey && !canSign -> true
            !isRejection && awaitingExecution && canExecute && nextInLine -> true

            else -> false
        }

    fun confirmationButtonIsEnabled(): Boolean = when {
        !confirmationButtonIsVisible() -> false
        isRejection && awaitingConfirmations && canSign -> true
        isRejection && awaitingConfirmations && hasOwnerKey && !canSign -> false

        !isRejection && awaitingConfirmations && canSign -> true

        awaitingExecution && canExecute && hasOwnerKey -> true

        else -> false
    }

    fun rejectionButtonIsVisible(): Boolean =
        when {
            isRejection && awaitingConfirmations && canSign -> false
            isRejection && awaitingConfirmations && hasOwnerKey && !canSign -> false

            !isRejection && awaitingConfirmations && hasOwnerKey && !hasBeenRejected -> true
            !isRejection && awaitingConfirmations && hasOwnerKey && hasBeenRejected -> true

            !isRejection && awaitingExecution && hasOwnerKey && !hasBeenRejected -> true
            !isRejection && awaitingExecution && hasOwnerKey && hasBeenRejected -> false

            else -> false
        }


    fun rejectionButtonIsEnabled(): Boolean =
        when {
            !rejectionButtonIsVisible() -> false

            !isRejection && awaitingConfirmations && hasOwnerKey && !hasBeenRejected -> true
            !isRejection && awaitingConfirmations && hasOwnerKey && hasBeenRejected -> false

            !isRejection && awaitingExecution && hasOwnerKey && !hasBeenRejected -> true

            else -> false
        }

    fun spacerIsVisible(): Boolean = confirmationButtonIsVisible() && rejectionButtonIsVisible()

    fun buttonContainerIsVisible(): Boolean = confirmationButtonIsVisible() || rejectionButtonIsVisible()
}
