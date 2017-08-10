package pm.gnosis.android.app.wallet.ui.multisig

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_add_multisig_text.view.*
import kotlinx.android.synthetic.main.fragment_multisig.*
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.db.MultisigWallet
import pm.gnosis.android.app.wallet.di.component.ApplicationComponent
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.base.BaseFragment
import pm.gnosis.android.app.wallet.util.snackbar
import timber.log.Timber
import javax.inject.Inject

class MultisigFragment : BaseFragment() {
    @Inject lateinit var presenter: MultisigPresenter
    @Inject lateinit var adapter: MultisigAdapter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater?.inflate(R.layout.fragment_multisig, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragment_multisig_wallets.layoutManager = LinearLayoutManager(context)
        fragment_multisig_wallets.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        fragment_multisig_input_address.setOnClickListener { showMultisigInputDialog() }
        disposables += presenter.observeMultisigList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onMultisigWallets, onError = this::onMultisigWalletsError)

        disposables += adapter.multisigSelection
                .subscribeBy(onNext = this::onMultisigSelection, onError = Timber::e)
    }

    private fun onMultisigWallets(wallets: List<MultisigWallet>) {
        adapter.setItems(wallets)
        fragment_multisig_empty_view.visibility = if (wallets.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onMultisigWalletsError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun showMultisigInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_multisig_text, null)
        fragment_multisig_fab.close(true)
        AlertDialog.Builder(context)
                .setTitle("Add a Multisig Wallet")
                .setView(dialogView)
                .setPositiveButton("Add", { _, _ ->
                    disposables += addMultisigWalletDisposable(
                            dialogView.dialog_add_multisig_text_name.text.toString(),
                            dialogView.dialog_add_multisig_text_address.text.toString())

                })
                .setNegativeButton("Cancel", { _, _ -> })
                .show()
    }

    private fun addMultisigWalletDisposable(name: String, address: String) =
            presenter.addMultisigWallet(name, address)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onMultisigWalletAdded, onError = this::onMultisigWalletAddError)

    private fun removeMultisigWalletDisposable(multisigWallet: MultisigWallet) =
            presenter.removeMultisigWallet(multisigWallet)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onMultisigWalletRemoved, onError = this::onMultisigWalletRemoveError)

    private fun updateMultisigWalletNameDisposable(address: String, newName: String) =
            presenter.updateMultisigWalletName(address, newName)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onComplete = this::onMultisigWalletNameChange, onError = this::onMultisigWalletNameChangeError)

    private fun onMultisigWalletAdded() {
        snackbar(fragment_multisig_coordinator_layout, "Added MultisigWallet")
    }

    private fun onMultisigWalletAddError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onMultisigWalletRemoved() {
        snackbar(fragment_multisig_coordinator_layout, "Removed Multisigwallet")
    }

    private fun onMultisigWalletRemoveError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onMultisigWalletNameChange() {
        snackbar(fragment_multisig_coordinator_layout, "Changed MultisigWallet name")
    }

    private fun onMultisigWalletNameChangeError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onMultisigSelection(multisigWallet: MultisigWallet) {
        val nameUpdate = if (multisigWallet.name.isNullOrEmpty()) "Add name" else "Change name"
        AlertDialog.Builder(context)
                .setTitle(multisigWallet.name)
                .setItems(arrayOf(nameUpdate, "Remove Multisig"), { dialog, which ->
                    when (which) {
                        0 -> showEditMultisigNameDialog(multisigWallet)
                        1 -> disposables += removeMultisigWalletDisposable(multisigWallet)
                    }
                }).show()
    }

    private fun showEditMultisigNameDialog(multisigWallet: MultisigWallet) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_multisig_text, null)
        dialogView.dialog_add_multisig_text_address.visibility = View.GONE
        AlertDialog.Builder(context)
                .setTitle(multisigWallet.name)
                .setView(dialogView)
                .setPositiveButton("Change name", { _, _ ->
                    multisigWallet.address?.let {
                        disposables += updateMultisigWalletNameDisposable(it,
                                dialogView.dialog_add_multisig_text_name.text.toString())
                    }
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
