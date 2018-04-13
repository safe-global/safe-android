package pm.gnosis.heimdall.ui.safe.add

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_address_input.view.*
import kotlinx.android.synthetic.main.layout_additional_owner_item.view.*
import kotlinx.android.synthetic.main.layout_address_item.view.*
import kotlinx.android.synthetic.main.layout_deploy_new_safe.*
import kotlinx.android.synthetic.main.layout_security_bars.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.ApplicationComponent
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.credits.BuyCreditsActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.utils.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.*
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import timber.log.Timber
import javax.inject.Inject

class DeployNewSafeFragment : BaseFragment() {

    @Inject
    lateinit var viewModel: AddSafeContract

    override fun onStart() {
        super.onStart()
        disposables += viewModel.setupDeploy()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { account ->
                layout_deploy_new_safe_device_info.apply {
                    layout_address_item_icon.setAddress(account)
                    layout_address_item_name.visible(true)
                    layout_address_item_name.text = getString(R.string.this_device)
                    layout_address_item_value.visible(false)
                }
            }
            .flatMapObservable {
                handleUserInput()
            }
            .subscribeBy(onError = Timber::e)

        layout_deploy_new_safe_deploy_button.isEnabled = false
        disposables += viewModel.observeAdditionalOwners()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                layout_deploy_new_safe_deploy_button.isEnabled = false
                layout_deploy_new_safe_balance_progress.visible(true)
            }
            .doOnNext(::updateOwners)
            .switchMapSingle {
                viewModel.estimateDeploy()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                layout_deploy_new_safe_balance_progress.visible(false)
            }
            .subscribeForResult(::displayFees, {
                Timber.e(it)
                errorSnackbar(layout_deploy_new_safe_name_input, it)
            })

        disposables += layout_deploy_new_safe_deploy_external.clicks()
            .flatMapSingle { viewModel.loadDeployData(layout_deploy_new_safe_name_input.text.toString()) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = {
                startActivityWithTransaction(it, onActivityNotFound = {
                    activity?.toast(R.string.no_external_wallets)
                    Timber.w("No activity resolved for intent")
                })
            }, onError = {
                Timber.e(it)
                errorSnackbar(layout_deploy_new_safe_name_input, it)
            })
    }

    private fun displayFees(fees: FeeEstimate) {
        layout_deploy_new_safe_current_balance_value.text = fees.balance.toString()
        layout_deploy_new_safe_costs_value.text = "- ${fees.costs}"
        val newBalance = fees.balance - fees.costs
        layout_deploy_new_safe_new_balance_value.text = newBalance.toString()
        layout_deploy_new_safe_deploy_button.isEnabled = newBalance >= 0
    }

    private fun handleUserInput() =
        Observable.merge(
            setupBuyCredits(),
            setupDeploySafe(),
            setupAddAddress()
        )

    private fun setupBuyCredits() =
        layout_deploy_new_safe_buy_credits_button.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { startActivity(BuyCreditsActivity.createIntent(context!!)) }

    private fun setupDeploySafe() =
        layout_deploy_new_safe_deploy_button.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapSingle {
                viewModel.deployNewSafe(layout_deploy_new_safe_name_input.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { toggleDeploying(true) }
                    .doAfterTerminate { toggleDeploying(false) }
            }
            .doOnNextForResult(::safeDeployed, ::errorDeploying)

    private fun toggleDeploying(inProgress: Boolean) {
        layout_deploy_new_safe_deploy_button.isEnabled = !inProgress
        layout_deploy_new_safe_name_input.isEnabled = !inProgress
        layout_deploy_new_safe_deploy_external.isEnabled = !inProgress
        layout_deploy_new_safe_add_owner_button.isEnabled = !inProgress
        layout_deploy_new_safe_progress.visible(inProgress)
    }

    private fun setupAddAddress() =
        layout_deploy_new_safe_add_owner_button.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                showAddOwnerDialog()
            }

    private fun showAddOwnerDialog() {
        // TODO: add proper dialog once design is known
        val dialogView = layoutInflater.inflate(R.layout.dialog_address_input, null)
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(R.string.add, { _, _ -> addOwner(dialogView.dialog_address_input_address.text.toString()) })
            .setNeutralButton(R.string.scan, { _, _ -> QRCodeScanActivity.startForResult(this) })
            .setOnDismissListener { activity?.hideSoftKeyboard() }
            .show()
    }

    private fun addOwner(address: String) {
        disposables += viewModel.addAdditionalOwner(address)
            .subscribeForResult(onError = { errorSnackbar(layout_deploy_new_safe_name_input, it) })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleTransactionHashResult(requestCode, resultCode, data,
            { transactionHash ->
                disposables += viewModel.saveTransactionHash(transactionHash, layout_deploy_new_safe_name_input.text.toString())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe { toggleDeploying(true) }
                    .doAfterTerminate { toggleDeploying(false) }
                    .subscribeBy(onComplete = { safeDeployed(transactionHash) }, onError = Timber::e)
            })

        handleQrCodeActivityResult(requestCode, resultCode, data,
            { parseEthereumAddress(it)?.let { addOwner(it.asEthereumAddressString()) } })
    }

    private fun safeDeployed(txHash: String) {
        startActivity(SafeMainActivity.createIntent(context!!, txHash.hexAsBigInteger()))
    }

    private fun errorDeploying(throwable: Throwable) {
        view?.let {
            if (throwable is IllegalStateException) {
                layout_deploy_new_safe_buy_credits_button.visible(true)
                layout_deploy_new_safe_deploy_button.visible(false)
                snackbar(it, R.string.error_not_enough_credits)
            } else {
                errorSnackbar(it, throwable)
            }
        }
    }

    private fun updateOwners(additionalOwners: List<Solidity.Address>) {
        layout_deploy_new_safe_additional_owners_container.removeAllViews()
        additionalOwners.forEach { address ->
            val view = layoutInflater.inflate(
                R.layout.layout_additional_owner_item,
                layout_deploy_new_safe_additional_owners_container,
                false
            )
            view.layout_address_item_value.text = address.asEthereumAddressString()
            view.layout_address_item_icon.setAddress(address)
            disposables += view.layout_additional_owner_delete_button.clicks()
                .flatMap { viewModel.removeAdditionalOwner(address) }
                .subscribeForResult(
                    {
                        snackbar(
                            layout_deploy_new_safe_additional_owners_container,
                            getString(R.string.removed_x, address)
                        )
                    },
                    { errorSnackbar(layout_deploy_new_safe_additional_owners_container, it) }
                )
            layout_deploy_new_safe_additional_owners_container.addView(view)
        }

        layout_deploy_new_safe_add_owner_button.visible(additionalOwners.size < 2)
        updateSecurityBar(additionalOwners.size)
    }

    private fun updateSecurityBar(additionalOwners: Int) {
        val securityInfoTextResource: Int
        val colorResource: Int
        val securityLevelTextResource: Int
        when (additionalOwners) {
            0 -> {
                securityInfoTextResource = R.string.security_info_weak
                colorResource = R.color.security_bar_low
                securityLevelTextResource = R.string.weak
            }
            1 -> {
                securityInfoTextResource = R.string.security_info_good
                colorResource = R.color.security_bar_good
                securityLevelTextResource = R.string.good
            }
            else -> {
                securityInfoTextResource = R.string.security_info_best
                colorResource = R.color.security_bar_best
                securityLevelTextResource = R.string.best
            }
        }

        layout_security_bars_first.setColorFilterCompat(if (additionalOwners >= 0) colorResource else R.color.security_bar_default)
        layout_security_bars_second.setColorFilterCompat(if (additionalOwners >= 1) colorResource else R.color.security_bar_default)
        layout_security_bars_third.setColorFilterCompat(if (additionalOwners >= 2) colorResource else R.color.security_bar_default)
        layout_deploy_new_safe_security_level_text.text = SpannableStringBuilder("")
            .appendText(
                getString(R.string.security_level),
                ForegroundColorSpan(context!!.getColorCompat(R.color.gnosis_dark_blue))
            )
            .append(": ")
            .appendText(
                getString(securityLevelTextResource),
                ForegroundColorSpan(context!!.getColorCompat(colorResource))
            )
        layout_deploy_new_safe_security_info.text = getString(securityInfoTextResource)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) =
        inflater.inflate(R.layout.layout_deploy_new_safe, container, false)!!

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
            .applicationComponent(component)
            .viewModule(ViewModule(context!!))
            .build().inject(this)
    }

    companion object {
        fun createInstance() = DeployNewSafeFragment()
    }
}
