package pm.gnosis.heimdall.data.repositories

import android.os.Bundle
import android.os.Parcelable
import io.reactivex.Single
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.TypeParceler
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.Operation
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.utils.SolidityAddressParceler
import pm.gnosis.model.Solidity
import java.math.BigInteger


interface TransactionInfoRepository {
    fun checkRestrictedTransaction(safe: Solidity.Address, transaction: SafeTransaction): Single<SafeTransaction>
    fun parseTransactionData(transaction: SafeTransaction): Single<TransactionData>
    fun loadTransactionInfo(id: String): Single<TransactionInfo>
}

sealed class RestrictedTransactionException(msg: String) : IllegalArgumentException(msg) {
    object DelegateCall : RestrictedTransactionException("Delegate calls are not allowed")
    object ModifyOwners : RestrictedTransactionException("Changing owners is not allowed")
    object ModifyModules : RestrictedTransactionException("Changing modules is not allowed")
    object ChangeThreshold : RestrictedTransactionException("Changing the threshold is not allowed")
    object ChangeMasterCopy : RestrictedTransactionException("Changing the master copy is not allowed")
    object SetFallbackHandler : RestrictedTransactionException("Setting the fallback handler is not allowed")
    object DataCallToSafe : RestrictedTransactionException("Interaction with the Safe are not allowed")
}

data class TransactionInfo(
    val id: String, val chainHash: String, val safe: Solidity.Address, val data: TransactionData, val timestamp: Long,
    val gasLimit: BigInteger, val gasPrice: BigInteger, val gasToken: Solidity.Address
)

sealed class TransactionData : Parcelable {
    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class Generic(val to: Solidity.Address, val value: BigInteger, val data: String?, val operation: Operation = Operation.CALL) :
        TransactionData()

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class ReplaceRecoveryPhrase(val safeTransaction: SafeTransaction) : TransactionData()

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class AssetTransfer(val token: Solidity.Address, val amount: BigInteger, val receiver: Solidity.Address) : TransactionData()

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class ConnectAuthenticator(val extension: Solidity.Address) : TransactionData()

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class UpdateMasterCopy(val masterCopy: Solidity.Address) : TransactionData()

    @Parcelize
    @TypeParceler<Solidity.Address, SolidityAddressParceler>
    data class MultiSend(val transactions: List<SafeTransaction>, val contract: Solidity.Address) : TransactionData()

    fun addToBundle(bundle: Bundle) =
        bundle.let {
            it.putInt(EXTRA_DATA_TYPE, getType())
            it.putParcelable(EXTRA_DATA, this)
        }

    private fun getType() =
        when (this) {
            is Generic -> TYPE_GENERIC
            is AssetTransfer -> TYPE_ASSET_TRANSFER
            is ReplaceRecoveryPhrase -> TYPE_REPLACE_RECOVERY_PHRASE
            is ConnectAuthenticator -> TYPE_CONNECT_EXTENSION
            is UpdateMasterCopy -> TYPE_UPDATE_MASTER_COPY
            is MultiSend -> TYPE_MULTI_SEND
        }

    companion object {
        private const val EXTRA_DATA_TYPE = "extra.int.data_type"
        private const val EXTRA_DATA = "extra.parcelable.data"

        private const val TYPE_GENERIC = 0
        private const val TYPE_ASSET_TRANSFER = 1
        private const val TYPE_REPLACE_RECOVERY_PHRASE = 2
        private const val TYPE_CONNECT_EXTENSION = 3
        private const val TYPE_UPDATE_MASTER_COPY = 4
        private const val TYPE_MULTI_SEND = 5

        fun fromBundle(bundle: Bundle): TransactionData? =
            bundle.run {
                when (getInt(EXTRA_DATA_TYPE, 0)) {
                    TYPE_GENERIC -> getParcelable<Generic>(EXTRA_DATA)
                    TYPE_ASSET_TRANSFER -> getParcelable<AssetTransfer>(EXTRA_DATA)
                    TYPE_REPLACE_RECOVERY_PHRASE -> getParcelable<ReplaceRecoveryPhrase>(EXTRA_DATA)
                    TYPE_CONNECT_EXTENSION -> getParcelable<ConnectAuthenticator>(EXTRA_DATA)
                    TYPE_UPDATE_MASTER_COPY -> getParcelable<UpdateMasterCopy>(EXTRA_DATA)
                    TYPE_MULTI_SEND -> getParcelable<MultiSend>(EXTRA_DATA)
                    else -> throw IllegalArgumentException("Unknown transaction data type")
                }
            }
    }
}

