package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.heimdall.ui.transactions.SubmitTransactionActivity
import pm.gnosis.heimdall.utils.GnosisSafeUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger


class CreateChangeSafeSettingsTransactionProgressDialog : BaseCreateSafeTransactionProgressDialog() {

    private var type: Int = 0
    private var ownerCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getInt(EXTRA_SETTINGS_TYPE) ?: 0
        ownerCount = arguments?.getInt(EXTRA_OWNER_COUNT) ?: 0

        if (type == 0 || ownerCount == 0) {
            dismiss()
        }
    }

    override fun createTransaction(): Single<Transaction> =
            Single.fromCallable {
                val data = when (type) {
                    SETTINGS_TYPE_ADD_OWNER -> {
                        val newThreshold = GnosisSafeUtils.calculateThresholdAsUInt8(ownerCount + 1)
                        GnosisSafe.AddOwner.encode(Solidity.Address(BigInteger.ZERO), newThreshold)
                    }
                    SETTINGS_TYPE_REMOVE_OWNER -> {
                        val ownerIndex = arguments?.getLong(EXTRA_OWNER_INDEX) ?: 0L
                        val newThreshold = GnosisSafeUtils.calculateThresholdAsUInt8(ownerCount - 1)
                        GnosisSafe.RemoveOwner.encode(Solidity.UInt256(BigInteger.valueOf(ownerIndex)), newThreshold)
                    }
                    SETTINGS_TYPE_REPLACE_OWNER -> {
                        val ownerIndex = arguments?.getLong(EXTRA_OWNER_INDEX) ?: 0L
                        GnosisSafe.ReplaceOwner.encode(
                                Solidity.UInt256(BigInteger.valueOf(ownerIndex)),
                                Solidity.Address(BigInteger.ZERO)
                        )
                    }
                    else -> throw UnsupportedOperationException()
                }
                Transaction(safeAddress!!, data = data)
            }.subscribeOn(Schedulers.computation())

    override fun showTransaction(safe: BigInteger?, transaction: Transaction) {
        startActivity(when (type) {
            SETTINGS_TYPE_REMOVE_OWNER ->
                SubmitTransactionActivity.createIntent(context!!, safe, transaction)
            else ->
                CreateTransactionActivity.createIntent(context!!, safe, mapType(), transaction)
        })
    }

    private fun mapType() = when (type) {
        SETTINGS_TYPE_ADD_OWNER -> TransactionType.ADD_SAFE_OWNER
        SETTINGS_TYPE_REMOVE_OWNER -> TransactionType.REMOVE_SAFE_OWNER
        SETTINGS_TYPE_REPLACE_OWNER -> TransactionType.REPLACE_SAFE_OWNER
        else -> TransactionType.GENERIC
    }

    companion object {
        private const val EXTRA_SETTINGS_TYPE = "extra.int.settings_type"
        private const val EXTRA_OWNER_COUNT = "extra.int.owner_count"
        private const val EXTRA_OWNER_INDEX = "extra.int.owner_index"

        private const val SETTINGS_TYPE_ADD_OWNER = 1
        private const val SETTINGS_TYPE_REMOVE_OWNER = 2
        private const val SETTINGS_TYPE_REPLACE_OWNER = 3

        fun addOwner(safeAddress: BigInteger, ownerCount: Int) =
                create(safeAddress, SETTINGS_TYPE_ADD_OWNER, ownerCount)

        fun removeOwner(safeAddress: BigInteger, ownerIndex: Long, ownerCount: Int) =
                create(safeAddress, SETTINGS_TYPE_REMOVE_OWNER, ownerCount, Bundle().apply {
                    putLong(EXTRA_OWNER_INDEX, ownerIndex)
                })

        fun replaceOwner(safeAddress: BigInteger, ownerIndex: Long, ownerCount: Int) =
                create(safeAddress, SETTINGS_TYPE_REPLACE_OWNER, ownerCount, Bundle().apply {
                    putLong(EXTRA_OWNER_INDEX, ownerIndex)
                })

        private fun create(safeAddress: BigInteger, type: Int, ownerCount: Int, params: Bundle? = null) =
                CreateChangeSafeSettingsTransactionProgressDialog().apply {
                    arguments = BaseCreateSafeTransactionProgressDialog.createBundle(safeAddress).apply {
                        putInt(EXTRA_SETTINGS_TYPE, type)
                        putInt(EXTRA_OWNER_COUNT, ownerCount)
                        params?.let { putAll(it) }
                    }
                }
    }
}
