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
    fun `(1_2) when (isRejection, awaitingConfirmations, canSign) should hide rejection and enable confirmation button`() {
//        val buttonStateHelper =
//            ButtonStateHelper2(
//                isRejection = true,
//                hasOwnerKey = true,
//                hasBeenRejected = true,
//                awaitingExecution = false,
//                awaitingConfirmations = false,
//                canSign = true,
//            )
//
//        assertTrue(buttonStateHelper.confirmationButtonIsVisible())
//        assertTrue(buttonStateHelper.confirmationButtonIsEnabled())
//        assertTrue(buttonStateHelper.buttonContainerIsVisible())
//
//        assertFalse(buttonStateHelper.rejectionButtonIsVisible())
//        assertFalse(buttonStateHelper.rejectionButtonIsEnabled())
    }


}
