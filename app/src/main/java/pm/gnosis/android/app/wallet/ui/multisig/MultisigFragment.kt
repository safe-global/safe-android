package pm.gnosis.android.app.wallet.ui.multisig

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater?.inflate(R.layout.fragment_multisig, container, false)

    override fun onStart() {
        super.onStart()
        fragment_multisig_input_address.setOnClickListener { showMultisigInputDialog() }

        disposables += presenter.observeMultisigList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onMultisigWallets, onError = this::onMultisigWalletsError)
    }

    private fun onMultisigWallets(wallets: List<MultisigWallet>) {
        Timber.d(wallets.toString())
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

    private fun onMultisigWalletAdded() {
        snackbar(fragment_multisig_coordinator_layout, "Added MultisigWallet")
    }

    private fun onMultisigWalletAddError(throwable: Throwable) {
        Timber.e(throwable)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(this.context))
                .build().inject(this)
    }
}
