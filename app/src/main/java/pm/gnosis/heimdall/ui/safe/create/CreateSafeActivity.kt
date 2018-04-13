package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_create_safe_v2.*
import kotlinx.android.synthetic.main.layout_deploy_new_safe.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.credits.BuyCreditsActivity
import pm.gnosis.heimdall.ui.safe.add.AddSafeContract
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import timber.log.Timber
import javax.inject.Inject

class CreateSafeActivity : BaseActivity() {
    override fun screenId(): ScreenId = ScreenId.CREATE_SAFE

    @Inject
    lateinit var viewModel: AddSafeContract

    private lateinit var chromeExtensionAddress: Solidity.Address
    private lateinit var recoveryAddress: Solidity.Address

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        chromeExtensionAddress = intent.getStringExtra(CHROME_EXTENSION_ADDRESS_EXTRA).asEthereumAddress() ?: run { finish(); return }
        recoveryAddress = intent.getStringExtra(RECOVERY_ADDRESS_EXTRA).asEthereumAddress() ?: run { finish(); return }

        inject()
        setContentView(R.layout.layout_create_safe_v2)
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_create_safe_buy_credits.clicks()
            .subscribeBy(onNext = { startActivity(BuyCreditsActivity.createIntent(this)) }, onError = Timber::e)

        //TODO: what if this fails
        disposables += viewModel.estimateDeploy()
            .toObservable() //lazy hack
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = ::displayFees, onError = Timber::e)

        disposables += layout_create_safe_button.clicks()
            .flatMapSingle { viewModel.deployNewSafe("Safe ${(0..10000).shuffled().first()}", setOf(chromeExtensionAddress, recoveryAddress)) }
            .subscribeForResult(onNext = ::safeDeployed, onError = ::errorDeploying)
    }

    private fun displayFees(fees: FeeEstimate) {
        layout_create_safe_current_balance_value.text = fees.balance.toString()
        layout_create_safe_costs_value.text = "- ${fees.costs}"
        val newBalance = fees.balance - fees.costs
        layout_create_safe_new_balance_value.text = newBalance.toString()
        layout_create_safe_button.isEnabled = newBalance >= 0
    }


    private fun safeDeployed(txHash: String) {
        startActivity(SafeMainActivity.createIntent(this, txHash.hexAsBigInteger()))
    }

    private fun errorDeploying(throwable: Throwable) {
        if (throwable is IllegalStateException) {
            layout_deploy_new_safe_buy_credits_button.visible(true)
            layout_deploy_new_safe_deploy_button.visible(false)
            snackbar(layout_create_safe_coordinator, R.string.error_not_enough_credits)
        } else {
            errorSnackbar(layout_create_safe_coordinator, throwable)
        }
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .applicationComponent(HeimdallApplication[this].component)
            .viewModule(ViewModule(this))
            .build()
            .inject(this)
    }

    companion object {
        private const val CHROME_EXTENSION_ADDRESS_EXTRA = "extra.string.chrome_extension_address"
        private const val RECOVERY_ADDRESS_EXTRA = "extra.string.recovery_address"

        fun createIntent(context: Context, chromeExtension: Solidity.Address, recovery: Solidity.Address) =
            Intent(context, CreateSafeActivity::class.java).apply {
                putExtra(CHROME_EXTENSION_ADDRESS_EXTRA, chromeExtension.asEthereumAddressString())
                putExtra(RECOVERY_ADDRESS_EXTRA, recovery.asEthereumAddressString())
            }
    }
}
