package pm.gnosis.heimdall.ui.transactions.details.safe

import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction

class ReviewChangeSafeSettingsDetailsFragment : ViewChangeSafeSettingsDetailsFragment() {

    override fun layout(): Int = R.layout.layout_review_change_safe

    override fun removedOwnerMessage(): Int = R.string.transaction_description_remove_safe_owner

    override fun addedOwnerMessage(): Int = R.string.transaction_description_add_safe_owner

    override fun replacedOwnerMessage(): Int = R.string.transaction_description_replace_safe_owner

    companion object {
        fun createInstance(transaction: SafeTransaction?, safeAddress: String?) =
            ReviewChangeSafeSettingsDetailsFragment().apply {
                arguments = createBundle(transaction, safeAddress)
            }
    }
}
