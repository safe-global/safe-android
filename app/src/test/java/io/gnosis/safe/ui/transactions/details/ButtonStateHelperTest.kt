package io.gnosis.safe.ui.transactions.details

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ButtonStateHelperTest {

    // For numbering, see: https://docs.google.com/document/d/13GirjSwieQaqUPz-8HXzJP_rfgmobSp7E4TpbnLlb0M/edit#
    @Test
    fun `(1_1) when (isRejection, awaitingConfirmations, canSign) should hide rejection and enable confirmation button`() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = true,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = true,
                canSign = true,
                canExecute = false,
                hasBeenRejected = true,
                awaitingExecution = false,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.confirmationButtonIsVisible())
        assertTrue(buttonStateHelper.confirmationButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())

        assertFalse(buttonStateHelper.rejectionButtonIsVisible())
        assertFalse(buttonStateHelper.rejectionButtonIsEnabled())
    }

    @Test
    fun `(1_2) when (isRejection && awaitingConfirmations && hasOwnerKey && !canSign) should disable confirmation button and hide rejection button`() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = true,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = true,
                canSign = false,
                canExecute = false,
                hasBeenRejected = true,
                awaitingExecution = !awaitingConfirmations,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.confirmationButtonIsVisible())
        assertFalse(buttonStateHelper.confirmationButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())

        assertFalse(buttonStateHelper.rejectionButtonIsVisible())
        assertFalse(buttonStateHelper.rejectionButtonIsEnabled())
    }

    @Test
    fun `(1_3) when (All other cases for rejection transaction) should hide both buttons`() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = true,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = false,
                canSign = false,
                canExecute = false,
                hasBeenRejected = true,
                awaitingExecution = !awaitingConfirmations,
                nextInLine = false
            )

        assertFalse(buttonStateHelper.buttonContainerIsVisible())
        assertFalse(buttonStateHelper.confirmationButtonIsVisible())
        assertFalse(buttonStateHelper.rejectionButtonIsVisible())
    }

    @Test
    fun `(2_1) when (!isRejection && awaitingConfirmations && canSign) should enable confirmation `() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = true,
                canSign = true,
                canExecute = false,
                hasBeenRejected = false,
                awaitingExecution = !awaitingConfirmations,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.confirmationButtonIsVisible())
        assertTrue(buttonStateHelper.confirmationButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())
    }

    @Test
    fun `(2_2) when (!isRejection && awaitingConfirmations && hasOwnerKey && !canSign && hasBeenRejected) should disable confirmation button`() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = true,
                canSign = false,
                canExecute = false,
                hasBeenRejected = true,
                awaitingExecution = !awaitingConfirmations,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.confirmationButtonIsVisible())
        assertFalse(buttonStateHelper.confirmationButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())
    }

    @Test
    fun `(2_2b) when (!isRejection && awaitingConfirmations && hasOwnerKey && !canSign && !hasBeenRejected) should disable confirmation button`() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = true,
                canSign = false,
                canExecute = false,
                hasBeenRejected = false,
                awaitingExecution = !awaitingConfirmations,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.confirmationButtonIsVisible())
        assertFalse(buttonStateHelper.confirmationButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())
    }

    @Test
    fun `(2_3) when (!isRejection && awaitingConfirmations && hasOwnerKey && !hasBeenRejected) should enable rejection button`() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = true,
                canSign = false,
                canExecute = false,
                hasBeenRejected = false,
                awaitingExecution = !awaitingConfirmations,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.rejectionButtonIsVisible())
        assertTrue(buttonStateHelper.rejectionButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())
    }

    @Test
    fun `(2_4) when (!isRejection && awaitingConfirmations && hasOwnerKey && hasBeenRejected) should disable rejection button`() {
        val awaitingConfirmations = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = awaitingConfirmations,
                hasOwnerKey = true,
                canSign = false,
                canExecute = false,
                hasBeenRejected = true,
                awaitingExecution = !awaitingConfirmations,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.rejectionButtonIsVisible())
        assertFalse(buttonStateHelper.rejectionButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())
    }

    @Test
    fun `(3_1) when (!isRejection && awaitingExecution && hasOwnerKey && !hasBeenRejected) should enable rejection button full-width`() {
        val awaitingExecution = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = !awaitingExecution,
                hasOwnerKey = true,
                canSign = false,
                canExecute = false,
                hasBeenRejected = false,
                awaitingExecution = awaitingExecution,
                nextInLine = false
            )

        assertTrue(buttonStateHelper.rejectionButtonIsVisible())
        assertTrue(buttonStateHelper.rejectionButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())

        assertFalse(buttonStateHelper.confirmationButtonIsVisible())
        assertFalse(buttonStateHelper.confirmationButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())

    }

    @Test
    fun `(3_2) when (!isRejection && awaitingExecution && hasOwnerKey && hasBeenRejected) should hide both buttons`() {
        val awaitingExecution = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = !awaitingExecution,
                hasOwnerKey = true,
                canSign = false,
                canExecute = false,
                hasBeenRejected = true,
                awaitingExecution = awaitingExecution,
                nextInLine = false
            )

        assertFalse(buttonStateHelper.buttonContainerIsVisible())
        assertFalse(buttonStateHelper.confirmationButtonIsVisible())
        assertFalse(buttonStateHelper.rejectionButtonIsVisible())
    }

    @Test
    fun `(3_3a) when (awaitingExecution) should hide both buttons `() {
        val awaitingExecution = true
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                awaitingConfirmations = !awaitingExecution,
                hasOwnerKey = false,
                canSign = false,
                canExecute = false,
                hasBeenRejected = true,
                awaitingExecution = awaitingExecution,
                nextInLine = false
            )

        assertFalse(buttonStateHelper.buttonContainerIsVisible())
        assertFalse(buttonStateHelper.confirmationButtonIsVisible())
        assertFalse(buttonStateHelper.rejectionButtonIsVisible())
    }
}
