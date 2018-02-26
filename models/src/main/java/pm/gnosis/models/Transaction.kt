package pm.gnosis.models

import java.math.BigInteger

data class Transaction(
    val address: BigInteger,
    val value: Wei? = null,
    var gas: BigInteger? = null,
    var gasPrice: BigInteger? = null,
    val data: String? = null,
    var nonce: BigInteger? = null,
    val chainId: Int = CHAIN_ID_ANY
) {
    fun signable() = nonce != null && gas != null && gasPrice != null

    fun parcelable() = TransactionParcelable(this)

    companion object {
        const val CHAIN_ID_ANY = 0
    }
}
