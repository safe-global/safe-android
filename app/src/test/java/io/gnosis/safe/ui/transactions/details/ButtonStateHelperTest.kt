package io.gnosis.safe.ui.transactions.details

import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.junit.Test

class ButtonStateHelperTest {

    // For numbering, see: https://docs.google.com/document/d/13GirjSwieQaqUPz-8HXzJP_rfgmobSp7E4TpbnLlb0M/edit#
    @Test
    fun `(1_1) when (isRejection, awaitingConfirmations, canSign) should hide rejection and enable confirmation button`() {
        val buttonStateHelper =
            ButtonStateHelper(
                isRejection = true,
                awaitingConfirmations = true,
                canSign = true,
                hasOwnerKey = true,
                hasBeenRejected = true,
                awaitingExecution = false
            )

        assertTrue(buttonStateHelper.confirmationButtonIsVisible())
        assertTrue(buttonStateHelper.confirmationButtonIsEnabled())
        assertTrue(buttonStateHelper.buttonContainerIsVisible())

        assertFalse(buttonStateHelper.rejectionButtonIsVisible())
        assertFalse(buttonStateHelper.rejectionButtonIsEnabled())
    }

    @Test
    fun `(1_2) when (isRejection && awaitingConfirmations && hasOwnerKey && !canSign) should disable confirmation button and hide rejection button`() {

    }

    @Test
    fun `(1_3) when (All other cases for rejection transaction) should hide both buttons`() {

    }


    @Test
    fun `(2_1) when (!isRejection && awaitingConfirmations && canSign) should enable confirmation `() {

    }

    @Test
    fun `(2_2) when (!isRejection && awaitingConfirmations && hasOwnerKey && !canSign) should disable confirmation button`() {

    }

    @Test
    fun `(2_3) when (!isRejection && awaitingConfirmations && hasOwnerKey && !hasBeenRejected) should enable rejection button`() {

    }

    @Test
    fun `(2_4) when (!isRejection && awaitingConfirmations && hasOwnerKey && hasBeenRejected) should disable rejection button`() {

    }
    @Test
    fun `(3_1) when (!isRejection && awaitingExecution && hasOwnerKey && !hasBeenRejected) should enable rejection button full-width`() {

    }

    @Test
    fun `(3_2) when (!isRejection && awaitingExecution && hasOwnerKey && hasBeenRejected) should hide both buttons`() {

    }

    @Test
    fun `(3_3) when (awaitingExecution) should hide both buttons `() {

    }

}
