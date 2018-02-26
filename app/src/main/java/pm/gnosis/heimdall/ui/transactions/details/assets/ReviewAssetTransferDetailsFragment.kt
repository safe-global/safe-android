package pm.gnosis.heimdall.ui.transactions.details.assets

import pm.gnosis.heimdall.R
import pm.gnosis.models.Transaction


class ReviewAssetTransferDetailsFragment : ViewAssetTransferDetailsFragment() {

    override fun layout(): Int = R.layout.layout_review_asset_transfer

    companion object {

        fun createInstance(transaction: Transaction?, safeAddress: String?) =
            ReviewAssetTransferDetailsFragment().apply {
                arguments = createBundle(transaction, safeAddress)
            }
    }
}