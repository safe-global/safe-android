package io.gnosis.safe.ui.transactions.details

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ButtonStateHelperTest {
    // Rejections
    @Test
    fun `when (isRejection, needsYourConfirmation, hasBeenRejected, !needsExecution) should show enabled confirmation Button and hide rejection button `() {

        val buttonStateHelper = ButtonStateHelper(isRejection = true, needsYourConfirmation = true, hasBeenRejected = true,   needsExecution = false)

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
    }

    @Test
    fun `when (isRejection, !needsYourConfirmation, hasBeenRejected, !needsExecution) should show no buttons `() {
        val buttonStateHelper = ButtonStateHelper(isRejection = true, needsYourConfirmation = false, hasBeenRejected = true,   needsExecution = false)

        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.buttonContainerIsVisible())

    }

    @Test // Only possible with missing rejectors in backend
    fun `when (isRejection, !needsYourConfirmation, !hasBeenRejected, !needsExecution) should show no buttons `() {
        val buttonStateHelper = ButtonStateHelper(isRejection = true, needsYourConfirmation = false, hasBeenRejected = false,   needsExecution = false)

        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.buttonContainerIsVisible())

    }

    // Non Rejections
    @Test
    fun `when (!isRejection, needsYourConfirmation, !hasBeenRejected, !needsExecution) should show both Buttons enabled`() {
        val buttonStateHelper = ButtonStateHelper(isRejection = false, needsYourConfirmation = true, hasBeenRejected = false, needsExecution = false)

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertTrue(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())

    }

    @Test
    fun `when (!isRejection, !needsYourConfirmation, !hasBeenRejected, needsExecution) should show reject button enabled and no spacer`() {
        val buttonStateHelper = ButtonStateHelper(isRejection = false, needsYourConfirmation = false, hasBeenRejected = false, needsExecution = true)

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())

    }


    @Test
    fun `when (!isRejection, !needsYourConfirmation, !hasBeenRejected, !needsExecution) should disable confirm button`() {
        val buttonStateHelper = ButtonStateHelper(isRejection = false, needsYourConfirmation = false, hasBeenRejected = false, needsExecution = false)

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertTrue(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())

    }

    @Test
    fun `when (!isRejection, !needsYourConfirmation, !hasBeenRejected, needsExecution) should hide confirm button`() {
        val buttonStateHelper = ButtonStateHelper(isRejection = false, needsYourConfirmation = false, hasBeenRejected = false, needsExecution = true)

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsVisible())
        assertTrue(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsVisible())
        assertFalse(buttonStateHelper.confirmButtonIsEnabled())
    }

    @Test
    fun `when (!isRejection, !needsYourConfirmation, hasBeenRejected, needsExecution) should hide reject button`() {
        val buttonStateHelper = ButtonStateHelper(isRejection = false, needsYourConfirmation = false, hasBeenRejected = true, needsExecution = true)

        assertTrue(buttonStateHelper.confirmButtonIsVisible())

        assertTrue(buttonStateHelper.buttonContainerIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsVisible())
        assertFalse(buttonStateHelper.rejectButtonIsEnabled())
        assertFalse(buttonStateHelper.spacerIsVisible())
        assertTrue(buttonStateHelper.confirmButtonIsEnabled())
    }
}
