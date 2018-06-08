package pm.gnosis.heimdall.data.repositories

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.StringRes
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.utils.SolidityAddressParceler
import pm.gnosis.model.Solidity
import java.math.BigInteger


interface TransactionInfoRepository {
    fun checkRestrictedTransaction(transaction: SafeTransaction): Single<SafeTransaction>
    fun parseTransactionData(transaction: SafeTransaction): Single<TransactionData>
    fun loadTransactionInfo(id: String): Single<TransactionInfo>
}

sealed class RestrictedTransactionException : IllegalArgumentException() {
    object DelegateCall: RestrictedTransactionException()
    object ModifyOwners: RestrictedTransactionException()
    object ModifyModules: RestrictedTransactionException()
}

data class TransactionInfo(
    val id: String, val chainHash: String, val safe: Solidity.Address, val data: TransactionData, val timestamp: Long,
    val gasLimit: BigInteger, val gasPrice: BigInteger, val gasToken: Solidity.Address
)

sealed class TransactionData : Parcelable {
    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class Generic(val to: Solidity.Address, val value: BigInteger, val data: String?) : TransactionData()

    /* Not used yet, they break the tests right now!
    @Parcelize
    @TypeParceler<Solidity.Address?, OptionalSolidityAddressParceler>
    data class RecoverSafe(val appAddress: Solidity.Address?, val extensionAddress: Solidity.Address?): TransactionData()

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class ReplaceRecoveryPhrase(val primaryKeyAddress: Solidity.Address, val secondaryKeyAddress: Solidity.Address): TransactionData()
    */

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class AssetTransfer(val token: Solidity.Address, val amount: BigInteger, val receiver: Solidity.Address) : TransactionData()

    fun addToBundle(bundle: Bundle) =
        bundle.let {
            it.putInt(EXTRA_DATA_TYPE, getType())
            it.putParcelable(EXTRA_DATA, this)
        }

    private fun getType() =
        when (this) {
            is Generic -> TYPE_GENERIC
            is AssetTransfer -> TYPE_ASSET_TRANSFER
        }

    companion object {
        private const val EXTRA_DATA_TYPE = "extra.int.data_type"
        private const val EXTRA_DATA = "extra.parcelable.data"

        private const val TYPE_GENERIC = 0
        private const val TYPE_ASSET_TRANSFER = 1

        fun fromBundle(bundle: Bundle): TransactionData? =
            bundle.run {
                when (getInt(TransactionData.EXTRA_DATA_TYPE, 0)) {
                    TransactionData.TYPE_GENERIC -> getParcelable<TransactionData.Generic>(TransactionData.EXTRA_DATA)
                    TransactionData.TYPE_ASSET_TRANSFER -> getParcelable<TransactionData.AssetTransfer>(TransactionData.EXTRA_DATA)
                    else -> throw IllegalArgumentException("Unknown transaction data type")
                }
            }
    }
}

