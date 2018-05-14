package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.heimdall.ui.transactions.SubmitTransactionActivity
import pm.gnosis.heimdall.utils.GnosisSafeUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
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

    override fun createTransaction(): Single<SafeTransaction> =
        Single.fromCallable {
            val data = when (type) {
                SETTINGS_TYPE_ADD_OWNER -> {
                    val newThreshold = GnosisSafeUtils.calculateThresholdAsUInt8(ownerCount + 1)
                    GnosisSafe.AddOwner.encode(Solidity.Address(BigInteger.ZERO), newThreshold)
                }
                SETTINGS_TYPE_REMOVE_OWNER -> {
                    val ownerIndex = arguments?.getLong(EXTRA_OWNER_INDEX) ?: throw IllegalArgumentException()
                    val owner = arguments?.getString(EXTRA_OWNER)?.asEthereumAddress() ?: throw IllegalArgumentException()
                    val newThreshold = GnosisSafeUtils.calculateThresholdAsUInt8(ownerCount - 1)
                    GnosisSafe.RemoveOwner.encode(Solidity.UInt256(BigInteger.valueOf(ownerIndex)), owner, newThreshold)
                }
                SETTINGS_TYPE_REPLACE_OWNER -> {
                    val ownerIndex = arguments?.getLong(EXTRA_OWNER_INDEX) ?: throw IllegalArgumentException()
                    val owner = arguments?.getString(EXTRA_OWNER)?.asEthereumAddress() ?: throw IllegalArgumentException()
                    GnosisSafe.ReplaceOwner.encode(
                        Solidity.UInt256(BigInteger.valueOf(ownerIndex)),
                        owner,
                        Solidity.Address(BigInteger.ZERO)
                    )
                }
                else -> throw UnsupportedOperationException()
            }
            SafeTransaction(Transaction(safeAddress!!, data = data), TransactionRepository.Operation.CALL)
        }.subscribeOn(Schedulers.computation())

    override fun showTransaction(safe: Solidity.Address?, transaction: SafeTransaction) {
        startActivity(
            when (type) {
                SETTINGS_TYPE_REMOVE_OWNER -> {
                    SubmitTransactionActivity.createIntent(context!!, safe, transaction)
                }
                else ->
                    CreateTransactionActivity.createIntent(context!!, safe, mapType(), transaction)
            }
        )
    }

    private fun mapType() = when (type) {
        SETTINGS_TYPE_ADD_OWNER -> TransactionType.ADD_SAFE_OWNER
        SETTINGS_TYPE_REMOVE_OWNER -> TransactionType.REMOVE_SAFE_OWNER
        SETTINGS_TYPE_REPLACE_OWNER -> TransactionType.REPLACE_SAFE_OWNER
        else -> TransactionType.GENERIC
    }

    companion object {
        private const val EXTRA_SETTINGS_TYPE = "extra.int.settings_type"
        private const val EXTRA_OWNER = "extra.string.owner"
        private const val EXTRA_OWNER_COUNT = "extra.int.owner_count"
        private const val EXTRA_OWNER_INDEX = "extra.int.owner_index"

        private const val SETTINGS_TYPE_ADD_OWNER = 1
        private const val SETTINGS_TYPE_REMOVE_OWNER = 2
        private const val SETTINGS_TYPE_REPLACE_OWNER = 3

        fun addOwner(safeAddress: Solidity.Address, ownerCount: Int) =
            create(safeAddress, SETTINGS_TYPE_ADD_OWNER, ownerCount)

        fun removeOwner(safeAddress: Solidity.Address, owner: Solidity.Address, ownerIndex: Long, ownerCount: Int) =
            create(safeAddress, SETTINGS_TYPE_REMOVE_OWNER, ownerCount, Bundle().apply {
                putString(EXTRA_OWNER, owner.asEthereumAddressString())
                putLong(EXTRA_OWNER_INDEX, ownerIndex)
            })

        fun replaceOwner(safeAddress: Solidity.Address, owner: Solidity.Address, ownerIndex: Long, ownerCount: Int) =
            create(safeAddress, SETTINGS_TYPE_REPLACE_OWNER, ownerCount, Bundle().apply {
                putString(EXTRA_OWNER, owner.asEthereumAddressString())
                putLong(EXTRA_OWNER_INDEX, ownerIndex)
            })

        private fun create(safeAddress: Solidity.Address, type: Int, ownerCount: Int, params: Bundle? = null) =
            CreateChangeSafeSettingsTransactionProgressDialog().apply {
                arguments = BaseCreateSafeTransactionProgressDialog.createBundle(safeAddress).apply {
                    putInt(EXTRA_SETTINGS_TYPE, type)
                    putInt(EXTRA_OWNER_COUNT, ownerCount)
                    params?.let { putAll(it) }
                }
            }
    }
}
