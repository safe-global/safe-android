package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction

class ReceiptAddRecoveryExtensionDetailsFragment : ViewAddRecoveryExtensionDetailsFragment() {

    override fun layout(): Int = R.layout.layout_receipt_add_recovery_extension

    companion object {
        fun createInstance(transaction: SafeTransaction?, safeAddress: String?) =
            ReceiptAddRecoveryExtensionDetailsFragment().apply {
                arguments = createBundle(transaction, safeAddress)
            }
    }
}
