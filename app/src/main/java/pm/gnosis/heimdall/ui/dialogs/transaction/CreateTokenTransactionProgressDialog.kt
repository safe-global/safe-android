package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject


class CreateTokenTransactionProgressDialog : BaseCreateSafeTransactionProgressDialog() {

    @Inject
    lateinit var viewModel: CreateTokenTransactionProgressContract

    private lateinit var tokenAddress: Solidity.Address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenAddress = arguments?.getString(TOKEN_ADDRESS_EXTRA)?.asEthereumAddress() ?: run { dismiss(); return }
        inject()
    }

    override fun createTransaction() = viewModel.loadCreateTokenTransaction(tokenAddress)

    override fun showTransaction(safe: Solidity.Address?, transaction: SafeTransaction) {
        startActivity(CreateTransactionActivity.createIntent(context!!, safe, TransactionType.TOKEN_TRANSFER, transaction))
    }

    fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!].component)
            .build()
            .inject(this)
    }

    companion object {
        private const val TOKEN_ADDRESS_EXTRA = "extra.string.token_address"

        fun create(safeAddress: Solidity.Address, tokenAddress: Solidity.Address) =
            CreateTokenTransactionProgressDialog().apply {
                arguments = BaseCreateSafeTransactionProgressDialog.createBundle(safeAddress).apply {
                    putString(TOKEN_ADDRESS_EXTRA, tokenAddress.asEthereumAddressString())
                }
            }
    }
}
