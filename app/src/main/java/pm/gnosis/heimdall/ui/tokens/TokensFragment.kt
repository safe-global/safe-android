package pm.gnosis.heimdall.ui.tokens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.dialog_token_add_input.view.*
import kotlinx.android.synthetic.main.dialog_token_info.view.*
import kotlinx.android.synthetic.main.layout_tokens.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.ApplicationComponent
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.*
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class TokensFragment : BaseFragment() {
    @Inject lateinit var viewModel: TokensContract
    @Inject lateinit var adapter: TokensAdapter

    private val removeTokenClickEvent = PublishSubject.create<ERC20Token>()
    private val addTokenClickEvent = PublishSubject.create<Pair<BigInteger, String?>>()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater?.inflate(R.layout.layout_tokens, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_tokens_fab.visibility = if (arguments?.getString(ARGUMENT_ADDRESS).isNullOrBlank()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        layout_tokens_list.layoutManager = LinearLayoutManager(context)
        layout_tokens_list.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)) {
                val scanResult = data.getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
                if (scanResult.isValidEthereumAddress()) {
                    showTokenAddressInputDialog(scanResult)
                } else {
                    snackbar(layout_tokens_coordinator_layout, R.string.invalid_ethereum_address)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                snackbar(layout_tokens_coordinator_layout, R.string.qr_code_scan_cancel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        disposables += layout_tokens_input_address.clicks()
                .subscribeBy(onNext = {
                    layout_tokens_fab.close(true)
                    showTokenAddressInputDialog()
                }, onError = Timber::e)

        disposables += layout_tokens_scan_qr_code.clicks()
                .subscribeBy(onNext = {
                    layout_tokens_fab.close(true)
                    scanQrCode()
                }, onError = Timber::e)

        disposables += addTokenDisposable()
        disposables += removeTokenDisposable()

        disposables += viewModel.observeTokens()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onTokensList, onError = this::onTokensListError)

        disposables += adapter.tokensSelectionSubject
                .flatMap {
                    viewModel.loadTokenInfo(it)
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe { onTokenInfoLoading(true) }
                            .doOnTerminate { onTokenInfoLoading(false) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onTokenInfo, onError = this::onTokenInfoError)

        disposables += adapter.tokenRemovalSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::showTokenRemovalDialog, onError = Timber::e)
    }

    private fun onTokensList(tokens: List<ERC20Token>) {
        adapter.setItems(tokens)
        layout_tokens_empty_view.visibility = if (tokens.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onTokensListError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onTokenInfo(token: ERC20Token) {
        if (token.decimals == null && token.name == null && token.symbol == null) {
            snackbar(layout_tokens_coordinator_layout, R.string.token_information_error)
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_token_info, null)

        if (token.decimals != null) {
            dialogView.dialog_token_info_decimals.text = token.decimals.asDecimalString()
        } else {
            dialogView.dialog_token_info_decimals_container.visibility = View.GONE
        }

        dialogView.dialog_token_info_address.text = token.address.asEthereumAddressString()
        dialogView.dialog_token_info_symbol.text = token.symbol

        AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle(token.name)
                .show()
    }

    private fun showTokenAddressInputDialog(withAddress: String? = null) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_token_add_input, null)

        if (withAddress != null) {
            dialogView.dialog_token_add_address.setText(withAddress)
            dialogView.dialog_token_add_address.isEnabled = false
            dialogView.dialog_token_add_address.inputType = InputType.TYPE_NULL
        }

        val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.token_add_dialog_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .setPositiveButton(R.string.add, { _, _ -> })
                .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val address = dialogView.dialog_token_add_address.text.toString()
            val name = dialogView.dialog_token_add_name.text.toString().let { if (it.isBlank()) null else it }
            if (address.isValidEthereumAddress()) {
                addTokenClickEvent.onNext(address.hexAsBigInteger() to name)
                dialog.dismiss()
            } else {
                context.toast(getString(R.string.invalid_ethereum_address))
            }
        }
    }

    private fun showTokenRemovalDialog(erC20Token: ERC20Token) {
        AlertDialog.Builder(context)
                .setTitle(getString(R.string.token_remove_dialog_title, erC20Token.name ?: getString(R.string.token)))
                .setMessage(getString(R.string.token_remove_dialog_description, if (erC20Token.name.isNullOrBlank()) erC20Token.address else erC20Token.name))
                .setPositiveButton(R.string.remove, { _, _ -> removeTokenClickEvent.onNext(erC20Token) })
                .setNegativeButton(R.string.cancel, { _, _ -> })
                .show()
    }

    private fun removeTokenDisposable() =
            removeTokenClickEvent.flatMap { viewModel.removeToken(it) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeForResult(onNext = this::onTokenRemoved, onError = this::onTokenRemoveError)

    private fun onTokenRemoved(erC20Token: ERC20Token) {
        val tokenDescription = if (erC20Token.name.isNullOrBlank()) getString(R.string.token) else erC20Token.name
        snackbar(layout_tokens_coordinator_layout, getString(R.string.token_removed_message, tokenDescription))
    }

    private fun onTokenRemoveError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_tokens_coordinator_layout, R.string.token_remove_error)
    }

    private fun onTokenInfoLoading(isLoading: Boolean) {
        adapter.itemsClickable = !isLoading
        layout_tokens_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun onTokenInfoError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_tokens_coordinator_layout, throwable)
    }

    private fun addTokenDisposable() = addTokenClickEvent
            .flatMap { viewModel.addToken(it.first, it.second) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = this::onTokenAdded, onError = this::onTokenAddError)

    private fun onTokenAdded(erC20Token: ERC20Token) {
        snackbar(layout_tokens_coordinator_layout,
                if (erC20Token.name.isNullOrBlank()) {
                    getString(R.string.token_added_message)
                } else {
                    getString(R.string.token_added_message_name, erC20Token.name)
                })
    }

    private fun onTokenAddError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_tokens_coordinator_layout, throwable)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(this.context))
                .build().inject(this)
    }

    companion object {
        private const val ARGUMENT_ADDRESS = "argument.string.address"

        fun createInstance(address: String) =
                TokensFragment().withArgs(Bundle().build { putString(ARGUMENT_ADDRESS, address) })
    }
}
