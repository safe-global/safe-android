package io.gnosis.safe.ui.settings.owner.ledger.transport

import java.lang.RuntimeException

class LedgerException : RuntimeException {
    enum class ExceptionReason {
        INVALID_PARAMETER,

        /** Returned if a parameter passed to a function is invalid  */
        IO_ERROR,

        /** Returned if the communication with the device fails  */
        APPLICATION_ERROR,

        /** Returned if an unexpected message is received from the device  */
        INTERNAL_ERROR
        /** Returned if an unexpected protocol error occurs when communicating with the device  */
    }

    var reason: ExceptionReason
        private set

    constructor(reason: ExceptionReason) {
        this.reason = reason
    }

    constructor(reason: ExceptionReason, details: String?) : super(details) {
        this.reason = reason
    }

    constructor(reason: ExceptionReason, cause: Throwable?) : super(cause) {
        this.reason = reason
    }

    override fun toString(): String {
        return reason.toString() + " " + super.toString()
    }
}
