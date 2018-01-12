package pm.gnosis.heimdall.ui.transactions.exceptions

import android.content.Context
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.exceptions.LocalizedException

data class TransactionInputException(private val internalMessage: String, val errorFields: Int, val showSnackbar: Boolean): Exception(internalMessage), LocalizedException {

    override fun localizedMessage(): String = internalMessage

    constructor(context: Context, errorFields: Int, showSnackbar: Boolean) : this(context.getString(R.string.error_transaction_params), errorFields, showSnackbar)

    companion object {
        // Transaction fields
        const val TO_FIELD = 1 shl 0
        const val VALUE_FIELD = 1 shl 1
        const val DATA_FIELD = 1 shl 2

        // Token specific fields
        const val TOKEN_FIELD = 1 shl 3
        const val AMOUNT_FIELD = 1 shl 4

        // Safe settings specific fields
        const val TARGET_FIELD = 1 shl 5

    }
}