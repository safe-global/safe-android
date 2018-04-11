package pm.gnosis.heimdall.ui.transactions.details.assets

import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.models.Transaction

class ReceiptAssetTransferDetailsFragment : ViewAssetTransferDetailsFragment() {

    override fun layout(): Int = R.layout.layout_receipt_asset_transfer

    companion object {
        fun createInstance(transaction: SafeTransaction?, safeAddress: String?) =
            ReceiptAssetTransferDetailsFragment().apply {
                arguments = createBundle(transaction, safeAddress)
            }
    }
}
