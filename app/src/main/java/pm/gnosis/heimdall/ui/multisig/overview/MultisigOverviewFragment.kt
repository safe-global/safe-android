package pm.gnosis.heimdall.ui.multisig.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_multisig_add_input.view.*
import kotlinx.android.synthetic.main.layout_multisig_overview.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.ApplicationComponent
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.ZxingIntentIntegrator.REQUEST_CODE
import pm.gnosis.heimdall.common.util.ZxingIntentIntegrator.SCAN_RESULT_EXTRA
import pm.gnosis.heimdall.common.util.scanQrCode
import pm.gnosis.heimdall.common.util.snackbar
import pm.gnosis.heimdall.common.util.subscribeForResult
import pm.gnosis.heimdall.common.util.toast
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.ui.multisig.details.MultisigDetailsActivity
import pm.gnosis.utils.addAddressPrefix
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class MultisigOverviewFragment : BaseFragment() {
    @Inject
    lateinit var viewModel: MultisigOverviewViewModel
    @Inject
    lateinit var adapter: MultisigAdapter
    @Inject
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater?.inflate(R.layout.layout_multisig_overview, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        layout_multisig_overview_wallets.layoutManager = layoutManager
        layout_multisig_overview_wallets.adapter = adapter
    }

    override fun onDestroyView() {
        layout_multisig_overview_wallets.layoutManager = null
        super.onDestroyView()
    }

    override fun onStart() {
        super.onStart()
        layout_multisig_overview_input_address.setOnClickListener {
            layout_multisig_overview_fab.close(true)
            showMultisigInputDialog()
        }

        layout_multisig_overview_scan_qr_code.setOnClickListener {
            layout_multisig_overview_fab.close(true)
            scanQrCode()
        }

        disposables += viewModel.observeMultisigWallets()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onMultisigWallets, onError = this::onMultisigWalletsError)

        disposables += adapter.multisigSelection
                .subscribeBy(onNext = this::onMultisigSelection, onError = Timber::e)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(SCAN_RESULT_EXTRA)) {
                val scanResult = data.getStringExtra(SCAN_RESULT_EXTRA)
                if (scanResult.isValidEthereumAddress()) {
                    showMultisigInputDialog(scanResult)
                } else {
                    snackbar(layout_multisig_overview_coordinator_layout, "Invalid address")
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                snackbar(layout_multisig_overview_coordinator_layout, "Cancelled by the user")
            }
        }
    }

    private fun onMultisigWallets(data: Adapter.Data<MultisigWallet>) {
        adapter.updateData(data)
        layout_multisig_overview_empty_view.visibility = if (data.entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onMultisigWalletsError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun showMultisigInputDialog(withAddress: String = "") {
        val dialogView = layoutInflater.inflate(R.layout.dialog_multisig_add_input, null)

        if (!withAddress.isEmpty()) {
            dialogView.dialog_add_multisig_text_address.setText(withAddress)
            dialogView.dialog_add_multisig_text_address.isEnabled = false
            dialogView.dialog_add_multisig_text_address.inputType = InputType.TYPE_NULL
        }

        val dialog = AlertDialog.Builder(context)
                .setTitle("Add a Multisig Wallet")
                .setView(dialogView)
                .setPositiveButton("Add", { _, _ -> })
                .setNegativeButton("Cancel", { _, _ -> })
                .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = dialogView.dialog_add_multisig_text_name.text.toString()
            val address = dialogView.dialog_add_multisig_text_address.text.toString()
            if (address.isValidEthereumAddress()) {
                disposables += addMultisigWalletDisposable(name, address.addAddressPrefix())
                dialog.dismiss()
            } else {
                context.toast("Invalid ethereum address")
            }
        }
    }

    private fun addMultisigWalletDisposable(name: String, address: String) =
            viewModel.addMultisigWallet(name, address)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onMultisigWalletAdded, onError = this::onMultisigWalletAddError)

    private fun removeMultisigWalletDisposable(address: String) =
            viewModel.removeMultisigWallet(address)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onMultisigWalletRemoved, onError = this::onMultisigWalletRemoveError)

    private fun updateMultisigWalletNameDisposable(address: String, newName: String) =
            viewModel.updateMultisigWalletName(address, newName)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onMultisigWalletNameChange, onError = this::onMultisigWalletNameChangeError)

    private fun onMultisigWalletAdded() {
        snackbar(layout_multisig_overview_coordinator_layout, "Added MultisigWallet")
    }

    private fun onMultisigWalletAddError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onMultisigWalletRemoved() {
        snackbar(layout_multisig_overview_coordinator_layout, "Removed Multisigwallet")
    }

    private fun onMultisigWalletRemoveError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onMultisigWalletNameChange() {
        snackbar(layout_multisig_overview_coordinator_layout, "Changed MultisigWallet name")
    }

    private fun onMultisigWalletNameChangeError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onMultisigSelection(multisigWallet: MultisigWallet) {
        startActivity(MultisigDetailsActivity.createIntent(context, multisigWallet))
    }

    private fun showEditMultisigNameDialog(multisigWallet: MultisigWallet) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_multisig_add_input, null)
        dialogView.dialog_add_multisig_text_address.visibility = View.GONE
        AlertDialog.Builder(context)
                .setTitle(multisigWallet.name)
                .setView(dialogView)
                .setPositiveButton("Change name", { _, _ ->
                    disposables += updateMultisigWalletNameDisposable(multisigWallet.address,
                            dialogView.dialog_add_multisig_text_name.text.toString())
                })
                .setNegativeButton("Cancel", { _, _ -> })
                .show()
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(this.context))
                .build().inject(this)
    }
}
