package io.gnosis.safe.ui.transactions.execution

import io.gnosis.safe.ScreenId
import io.gnosis.safe.ui.transactions.details.AdvancedTransactionDetailsFragment

class TxAdvancedParamsFragment : AdvancedTransactionDetailsFragment() {
    override fun screenId() = ScreenId.TRANSACTIONS_EXEC_REVIEW_ADVANCED
}
