package pm.gnosis.android.app.wallet.data.model

data class TransferDetails(val address: String,
                           val value: String? = null,
                           val gas: String? = null,
                           val data: String? = null)