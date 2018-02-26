package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigInteger
import javax.inject.Inject


class CreateTokenTransactionProgressDialog : BaseCreateSafeTransactionProgressDialog() {

    @Inject
    lateinit var viewModel: CreateTokenTransactionProgressContract

    private var tokenAddress: BigInteger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenAddress = arguments?.getString(TOKEN_ADDRESS_EXTRA)?.hexAsEthereumAddressOrNull()
        inject()
    }

    override fun createTransaction() =
        viewModel.loadCreateTokenTransaction(tokenAddress)

    override fun showTransaction(safe: BigInteger?, transaction: Transaction) {
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

        fun create(safeAddress: BigInteger, tokenAddress: BigInteger) =
            CreateTokenTransactionProgressDialog().apply {
                arguments = BaseCreateSafeTransactionProgressDialog.createBundle(safeAddress).apply {
                    putString(TOKEN_ADDRESS_EXTRA, tokenAddress.asEthereumAddressString())
                }
            }
    }
}