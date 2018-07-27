package pm.gnosis.heimdall.ui.safe.recover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_replace_browser_extension.*
import kotlinx.android.synthetic.main.layout_replace_browser_extension_info.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.nullOnThrow
import pm.gnosis.utils.stringWithNoTrailingZeroes
import timber.log.Timber
import java.math.BigInteger

class ReplaceBrowserExtensionActivity : ViewModelActivity<ReplaceBrowserExtensionContract>() {
    private var submissionInProgress = false

    override fun layout() = R.layout.layout_replace_browser_extension

    override fun inject(component: ViewComponent) = viewComponent().inject(this)

    override fun screenId() = ScreenId.REPLACE_BROWSER_EXTENSION_REVIEW

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val txGas = nullOnThrow { intent.getStringExtra(EXTRA_TX_GAS)?.toBigInteger() } ?: run { finish(); return }
        val dataGas = nullOnThrow { intent.getStringExtra(EXTRA_DATA_GAS)?.toBigInteger() } ?: run { finish(); return }
        val gasPrice = nullOnThrow { intent.getStringExtra(EXTRA_GAS_PRICE)?.toBigInteger() } ?: run { finish(); return }
        val safeTransaction = intent.getParcelableExtra<SafeTransaction>(EXTRA_SAFE_TRANSACTION) ?: run { finish(); return }
        val signature1 =
            nullOnThrow { intent.getStringExtra(EXTRA_ADDRESS_1).asEthereumAddress()!! to Signature.from(intent.getStringExtra(EXTRA_SIGNATURE_1)) }
                ?: run { finish(); return }
        val signature2 =
            nullOnThrow { intent.getStringExtra(EXTRA_ADDRESS_2).asEthereumAddress()!! to Signature.from(intent.getStringExtra(EXTRA_SIGNATURE_2)) }
                ?: run { finish(); return }

        val chromeExtensionAddress =
            nullOnThrow { intent.getStringExtra(EXTRA_CHROME_EXTENSION_ADDRESS).asEthereumAddress()!! } ?: run { finish(); return }

        viewModel.setup(safeTransaction, signature1, signature2, txGas, dataGas, gasPrice, chromeExtensionAddress)

        layout_replace_browser_extension_fee.text = "${viewModel.getMaxTransactionFee().toEther().stringWithNoTrailingZeroes()} ETH"
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.loadSafe()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = { safe ->
                layout_replace_browser_extension_info_safe_image.setAddress(safe.address)
                layout_replace_browser_extension_info_safe_address.text =
                        safe.address.asEthereumAddressString().let { "${it.subSequence(0, 6)}...${it.subSequence(it.length - 6, it.length)}" }
                layout_replace_browser_extension_info_safe_name.text = safe.displayName(this)
            }, onError = Timber::e)

        disposables += viewModel.observeSafeBalance()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = ::onSafeBalance, onError = ::onSafeBalanceError)

        disposables += layout_replace_browser_extension_submit.clicks()
            .flatMapSingle { _ ->
                viewModel.submitTransaction()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { onSubmitTransactionProgress(true) }
                    .doAfterTerminate { onSubmitTransactionProgress(false) }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                startActivity(SafeMainActivity.createIntent(this, viewModel.getSafeTransaction().wrapped.address, selectedTab = 1))
            }, onError = ::onSubmitTransactionError)

        disposables += layout_replace_browser_extension_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)
    }

    private fun onSafeBalance(safeBalance: Wei) {
        layout_replace_browser_extension_submit.isEnabled = safeBalance >= viewModel.getMaxTransactionFee() && !submissionInProgress
        layout_replace_browser_extension_safe_balance.text = "${safeBalance.toEther().stringWithNoTrailingZeroes()} ETH"
    }

    private fun onSafeBalanceError(throwable: Throwable) {
        Timber.e(throwable)
        layout_replace_browser_extension_safe_balance.text = "-"
    }

    private fun onSubmitTransactionProgress(isLoading: Boolean) {
        submissionInProgress = isLoading
        layout_replace_browser_extension_progress.visible(isLoading)
        layout_replace_browser_extension_submit.isEnabled = !isLoading
    }

    private fun onSubmitTransactionError(throwable: Throwable) {
        errorSnackbar(layout_replace_browser_extension_coordinator, throwable)
    }

    companion object {
        private const val EXTRA_SAFE_TRANSACTION = "extra.parcelable.safe_transaction"
        private const val EXTRA_ADDRESS_1 = "extra.string.address1"
        private const val EXTRA_ADDRESS_2 = "extra.string.address2"
        private const val EXTRA_SIGNATURE_1 = "extra.string.signature1"
        private const val EXTRA_SIGNATURE_2 = "extra.string.signature2"
        private const val EXTRA_TX_GAS = "extra.string.tx_gas"
        private const val EXTRA_DATA_GAS = "extra.string.data_gas"
        private const val EXTRA_GAS_PRICE = "extra.string.gas_price"
        private const val EXTRA_CHROME_EXTENSION_ADDRESS = "extra.string.chrome_extension_address"

        fun createIntent(
            context: Context,
            safeTransaction: SafeTransaction,
            signature1: Pair<Solidity.Address, Signature>,
            signature2: Pair<Solidity.Address, Signature>,
            txGas: BigInteger,
            dataGas: BigInteger,
            gasPrice: BigInteger,
            chromeExtensionAddress: Solidity.Address
        ) =
            Intent(context, ReplaceBrowserExtensionActivity::class.java).apply {
                putExtra(EXTRA_SAFE_TRANSACTION, safeTransaction)
                putExtra(EXTRA_ADDRESS_1, signature1.first.asEthereumAddressString())
                putExtra(EXTRA_ADDRESS_2, signature2.first.asEthereumAddressString())
                putExtra(EXTRA_SIGNATURE_1, signature1.second.toString())
                putExtra(EXTRA_SIGNATURE_2, signature2.second.toString())
                putExtra(EXTRA_TX_GAS, txGas.toString())
                putExtra(EXTRA_DATA_GAS, dataGas.toString())
                putExtra(EXTRA_GAS_PRICE, gasPrice.toString())
                putExtra(EXTRA_CHROME_EXTENSION_ADDRESS, chromeExtensionAddress.asEthereumAddressString())
            }
    }
}
