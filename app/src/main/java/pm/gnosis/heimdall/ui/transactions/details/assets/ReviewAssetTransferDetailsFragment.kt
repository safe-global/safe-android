package pm.gnosis.heimdall.ui.transactions.details.assets

import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction

class ReviewAssetTransferDetailsFragment : ViewAssetTransferDetailsFragment() {

    override fun layout(): Int = R.layout.layout_review_asset_transfer

    companion object {
        fun createInstance(transaction: SafeTransaction?, safeAddress: String?) =
            ReviewAssetTransferDetailsFragment().apply {
                arguments = createBundle(transaction, safeAddress)
            }
    }
}
