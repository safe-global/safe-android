package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import io.reactivex.Single
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.GnosisSafeExtensionRepository
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger
import javax.inject.Inject


class CreateAddExtensionTransactionProgressDialog : BaseCreateSafeTransactionProgressDialog() {

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
        extensionRepository.buildAddRecoverExtensionTransaction(emptyList())

    override fun showTransaction(safe: Solidity.Address?, transaction: SafeTransaction) {
        startActivity(CreateTransactionActivity.createIntent(context!!, safe, mapType(), transaction))
    }

    private fun mapType() = when (type) {
        SETTINGS_TYPE_ADD_RECOVERY_EXTENSION -> TransactionType.ADD_RECOVERY_EXTENSION
        else -> TransactionType.GENERIC
    }

    companion object {
        private const val EXTRA_SETTINGS_TYPE = "extra.int.settings_type"

        private const val SETTINGS_TYPE_ADD_RECOVERY_EXTENSION = 1

        fun addRecoveryExtension(safeAddress: Solidity.Address) =
            create(safeAddress, SETTINGS_TYPE_ADD_RECOVERY_EXTENSION)

        private fun create(safeAddress: Solidity.Address, type: Int, params: Bundle? = null) =
            CreateAddExtensionTransactionProgressDialog().apply {
                arguments = BaseCreateSafeTransactionProgressDialog.createBundle(safeAddress).apply {
                    putInt(EXTRA_SETTINGS_TYPE, type)
                    params?.let { putAll(it) }
                }
            }
    }
}
