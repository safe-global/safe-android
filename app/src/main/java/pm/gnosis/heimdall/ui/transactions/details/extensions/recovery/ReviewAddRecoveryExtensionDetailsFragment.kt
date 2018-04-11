package pm.gnosis.heimdall.ui.transactions.details.extensions.recovery

import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction

class ReviewAddRecoveryExtensionDetailsFragment : ViewAddRecoveryExtensionDetailsFragment() {

    override fun layout(): Int = R.layout.layout_review_add_recovery_extension

    companion object {
        fun createInstance(transaction: SafeTransaction?, safeAddress: String?) =
            ReviewAddRecoveryExtensionDetailsFragment().apply {
                arguments = createBundle(transaction, safeAddress)
            }
    }
}
