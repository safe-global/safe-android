package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import io.reactivex.Single
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.data.repositories.GnosisSafeExtensionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger
import javax.inject.Inject


class CreateChangeExtensionTransactionProgressDialog : BaseCreateSafeTransactionProgressDialog() {

    @Inject
    lateinit var extensionRepository: GnosisSafeExtensionRepository

    private var type: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getInt(EXTRA_SETTINGS_TYPE) ?: 0

        if (type == 0) {
            dismiss()
        }
        inject()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!].component)
            .build()
            .inject(this)
    }

    override fun createTransaction(): Single<SafeTransaction> =
        when (type) {
            SETTINGS_TYPE_ADD_RECOVERY_EXTENSION -> extensionRepository.buildAddRecoverExtensionTransaction(Solidity.Address(BigInteger.ZERO))
            SETTINGS_TYPE_REMOVE_EXTENSION -> loadRemoveExtensionTransaction()
            else -> Single.error(IllegalArgumentException())
        }

    private fun loadRemoveExtensionTransaction(): Single<SafeTransaction> {
        val safe = safeAddress ?: return Single.error(IllegalArgumentException())
        val extension = arguments?.getString(EXTRA_EXTENSION_ADDRESS)?.asEthereumAddress() ?: return Single.error(IllegalArgumentException())
        val index =
            arguments?.getInt(EXTRA_EXTENSION_INDEX)?.let { BigInteger.valueOf(it.toLong()) } ?: return Single.error(IllegalArgumentException())
        return extensionRepository.buildRemoveExtensionTransaction(safe, index, extension)
    }

    override fun showTransaction(safe: Solidity.Address?, transaction: SafeTransaction) {
        startActivity(CreateTransactionActivity.createIntent(context!!, safe, mapType(), transaction))
    }

    private fun mapType() = when (type) {
        SETTINGS_TYPE_ADD_RECOVERY_EXTENSION -> TransactionType.ADD_RECOVERY_EXTENSION
        SETTINGS_TYPE_REMOVE_EXTENSION -> TransactionType.REMOVE_EXTENSION
        else -> TransactionType.GENERIC
    }

    companion object {
        private const val EXTRA_SETTINGS_TYPE = "extra.int.settings_type"
        private const val EXTRA_EXTENSION_ADDRESS = "extra.string.extension_address"
        private const val EXTRA_EXTENSION_INDEX = "extra.string.extension_index"

        private const val SETTINGS_TYPE_ADD_RECOVERY_EXTENSION = 1
        private const val SETTINGS_TYPE_REMOVE_EXTENSION = 42

        fun addRecoveryExtension(safeAddress: Solidity.Address) =
            create(safeAddress, SETTINGS_TYPE_ADD_RECOVERY_EXTENSION)

        fun removeExtension(safeAddress: Solidity.Address, extension: Solidity.Address, index: Int) =
            create(safeAddress, SETTINGS_TYPE_REMOVE_EXTENSION, Bundle().apply {
                putString(EXTRA_EXTENSION_ADDRESS, extension.asEthereumAddressString())
                putInt(EXTRA_EXTENSION_INDEX, index)
            })

        private fun create(safeAddress: Solidity.Address, type: Int, params: Bundle? = null) =
            CreateChangeExtensionTransactionProgressDialog().apply {
                arguments = BaseCreateSafeTransactionProgressDialog.createBundle(safeAddress).apply {
                    putInt(EXTRA_SETTINGS_TYPE, type)
                    params?.let { putAll(it) }
                }
            }
    }
}
