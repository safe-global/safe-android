package io.gnosis.safe.ui.transactions.details

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ButtonStateHelperTest {

    // Rejections For numbering, see: https://docs.google.com/document/d/1dlhVAv9WiD1EPi1LuZI6opk17ivD2lpMbwbWP4aArIY/edit
    @Test
    fun `(1) when (isRejection, !needsYourConfirmation, hasBeenRejected, !needsExecution) should hide all buttons`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = true,
                needsYourConfirmation = false,
                hasBeenRejected = true,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.buttonContainerIsVisible())

    }

    @Test
    fun `(2) when (isRejection, needsYourConfirmation, hasBeenRejected, !needsExecution) should show enabled confirmation Button and hide rejection button `() {

        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = true,
                needsYourConfirmation = true,
                hasBeenRejected = true,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
    }

    @Test // Only possible with missing rejectors in backend
    fun `when (isRejection, !needsYourConfirmation, !hasBeenRejected, !needsExecution) should show no buttons `() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = true,
                needsYourConfirmation = false,
                hasBeenRejected = false,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.buttonContainerIsVisible())
    }

    // Non Rejections
    @Test
    fun `(3) when (!isRejection, needsYourConfirmation, !hasBeenRejected, !needsExecution) should show both Buttons enabled`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = true,
                hasBeenRejected = false,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertTrue(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())

    }

    @Test
    fun `(4) when (!isRejection, !needsYourConfirmation, !hasBeenRejected, !needsExecution) should disable confirm button`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = false,
                hasBeenRejected = false,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertTrue(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())

    }

    @Test
    fun `(5) when (!isRejection, needsYourConfirmation, !hasBeenRejected, !needsExecution) should enable confirm button`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = true,
                hasBeenRejected = false,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertTrue(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())
    }

    @Test
    fun `(6) when (!isRejection, needsYourConfirmation, hasBeenRejected, !needsExecution) shoild diable reject button `() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = true,
                hasBeenRejected = true,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
        assertTrue(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())
    }

    @Test
    fun `(7) when (!isRejection, !needsYourConfirmation, !hasBeenRejected, !needsExecution) should enable reject button`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = true,
                hasBeenRejected = false,
                needsExecution = false,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertTrue(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())
    }

    @Test
    fun `(8) + (10) when (!isRejection, !needsYourConfirmation, !hasBeenRejected, needsExecution) should hide confirm button`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = false,
                hasBeenRejected = false,
                needsExecution = true,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())
    }

    @Test
    fun `(9) when (!isRejection, !needsYourConfirmation, hasBeenRejected, needsExecution) should hide reject and confirm button`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = false,
                hasBeenRejected = true,
                needsExecution = true,
                canReject = true,
                isOwner = true,
                completed = false
            )

        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsVisible())

        assertFalse(buttonStateHelper.buttonContainerIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())
    }

    @Test
    fun `(11) when (!isRejection, !needsYourConfirmation, hasBeenRejected, needsExecution, !canReject, !isOwner) should hide both buttons`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = false,
                hasBeenRejected = true,
                needsExecution = true,
                canReject = false,
                isOwner = false,
                completed = false
            )

        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())
        assertFalse(buttonStateHelper.buttonContainerIsVisible())
    }

    @Test
    fun `(11b) when (!isOwner, !completed) should hide both buttons`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = false,
                hasBeenRejected = false,
                needsExecution = true,
                canReject = false,
                isOwner = false,
                completed = false
            )

        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())
        assertFalse(buttonStateHelper.buttonContainerIsVisible())
    }

    @Test
    fun `(11c) when (completed) should hide both buttons`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = false,
                needsYourConfirmation = false,
                hasBeenRejected = false,
                needsExecution = true,
                canReject = false,
                isOwner = true,
                completed = false
            )

        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())
        assertFalse(buttonStateHelper.buttonContainerIsVisible())
    }
}
