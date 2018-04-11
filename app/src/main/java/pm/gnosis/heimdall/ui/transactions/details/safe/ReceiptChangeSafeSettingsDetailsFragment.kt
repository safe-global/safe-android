package pm.gnosis.heimdall.ui.transactions.details.safe

import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction

class ReceiptChangeSafeSettingsDetailsFragment : ViewChangeSafeSettingsDetailsFragment() {

    override fun layout(): Int = R.layout.layout_receipt_change_safe

    override fun removedOwnerMessage(): Int = R.string.transaction_description_removed_safe_owner

    override fun addedOwnerMessage(): Int = R.string.transaction_description_added_safe_owner

    override fun replacedOwnerMessage(): Int = R.string.transaction_description_replaced_safe_owner

    companion object {
        fun createInstance(transaction: SafeTransaction?, safeAddress: String?) =
            ReceiptChangeSafeSettingsDetailsFragment().apply {
                arguments = createBundle(transaction, safeAddress)
            }
    }
}
