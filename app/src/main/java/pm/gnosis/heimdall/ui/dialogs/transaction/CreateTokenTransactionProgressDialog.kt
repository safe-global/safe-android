package pm.gnosis.heimdall.ui.dialogs.transaction

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.TransactionType
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.heimdall.ui.transactions.CreateTransactionActivity
import pm.gnosis.heimdall.utils.errorToast
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import java.math.BigInteger
import javax.inject.Inject


class CreateTokenTransactionProgressDialog : BaseDialog() {

    @Inject
    lateinit var viewModel: CreateTokenTransactionProgressContract

    private var safeAddress: BigInteger? = null
    private var tokenAddress: BigInteger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        safeAddress = arguments?.getString(SAFE_ADDRESS_EXTRA)?.hexAsEthereumAddressOrNull()
        tokenAddress = arguments?.getString(TOKEN_ADDRESS_EXTRA)?.hexAsEthereumAddressOrNull()
        inject()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.dialog_create_token_transaction_progress, container, false)

    override fun onStart() {
        super.onStart()
        disposables += viewModel.loadCreateTokenTransaction(tokenAddress)
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    dismiss()
                }
                .subscribe({
                    startActivity(CreateTransactionActivity.createIntent(context!!, safeAddress, TransactionType.TOKEN_TRANSFER, it))
                }, {
                    context!!.errorToast(it)
                })
    }

    fun inject() {
        DaggerViewComponent.builder()
                .viewModule(ViewModule(context!!))
                .applicationComponent(HeimdallApplication[context!!].component)
                .build()
                .inject(this)
    }

    companion object {
        const val SAFE_ADDRESS_EXTRA = "extra.string.safe_address"
        const val TOKEN_ADDRESS_EXTRA = "extra.string.token_address"

        fun create(safeAddress: BigInteger, tokenAddress: BigInteger): CreateTokenTransactionProgressDialog {
            val bundle = Bundle()
            bundle.putString(SAFE_ADDRESS_EXTRA, safeAddress.asEthereumAddressString())
            bundle.putString(TOKEN_ADDRESS_EXTRA, tokenAddress.asEthereumAddressString())
            return CreateTokenTransactionProgressDialog().apply { arguments = bundle }
        }
    }
}