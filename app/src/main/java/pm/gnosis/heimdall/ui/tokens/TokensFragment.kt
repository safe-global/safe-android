package pm.gnosis.heimdall.ui.tokens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.layout_token_info.view.*
import kotlinx.android.synthetic.main.layout_tokens.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.component.ApplicationComponent
import pm.gnosis.heimdall.common.di.component.DaggerViewComponent
import pm.gnosis.heimdall.common.di.module.ViewModule
import pm.gnosis.heimdall.common.util.*
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseFragment
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.isValidEthereumAddress
import timber.log.Timber
import javax.inject.Inject

class TokensFragment : BaseFragment() {
    @Inject lateinit var viewModel: TokensContract
    @Inject lateinit var adapter: TokensAdapter

    private val removeTokenClickEvent = PublishSubject.create<ERC20Token>()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater?.inflate(R.layout.layout_tokens, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layout_tokens_fab.visibility =
                if (arguments?.getString(ARGUMENT_ADDRESS).isNullOrBlank()) View.VISIBLE
                else View.GONE

        layout_tokens_list.layoutManager = LinearLayoutManager(context)
        layout_tokens_list.adapter = adapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ZxingIntentIntegrator.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)) {
                val scanResult = data.getStringExtra(ZxingIntentIntegrator.SCAN_RESULT_EXTRA)
                if (scanResult.isValidEthereumAddress()) {
                    startActivity(AddTokenActivity.createIntent(activity, scanResult))
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
                    startActivity(AddTokenActivity.createIntent(activity))
                }, onError = Timber::e)

        disposables += layout_tokens_scan_qr_code.clicks()
                .subscribeBy(onNext = {
                    layout_tokens_fab.close(true)
                    scanQrCode()
                }, onError = Timber::e)

        disposables += removeTokenDisposable()

        disposables += viewModel.observeLoadingStatus()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = {
                    layout_tokens_swipe_refresh.isRefreshing = it
                }, onError = Timber::e)

        disposables += viewModel.observeTokens(layout_tokens_swipe_refresh.refreshes())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onTokensList, onError = this::onTokensListError)

        disposables += adapter.tokensSelectionSubject
                .flatMap { viewModel.loadTokenInfo(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeForResult(onNext = this::onTokenInfo, onError = this::onTokenInfoError)

        disposables += adapter.tokenRemovalSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::showTokenRemovalDialog, onError = Timber::e)
    }

    private fun onTokensList(tokens: Adapter.Data<TokensContract.ERC20TokenWithBalance>) {
        adapter.updateData(tokens)
        layout_tokens_empty_view.visibility = if (tokens.entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onTokensListError(throwable: Throwable) {
        Timber.e(throwable)
        errorSnackbar(layout_tokens_coordinator_layout, throwable)
    }

    private fun onTokenInfo(token: ERC20Token) {
        if (token.name == null && token.symbol == null) {
            snackbar(layout_tokens_coordinator_layout, R.string.token_information_error)
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.layout_token_info, null)
        dialogView.layout_token_info_decimals.text = token.decimals.toString()
        dialogView.layout_token_info_name.text = token.address.asEthereumAddressString()
        dialogView.layout_token_info_symbol.text = token.symbol

        AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle(token.name)
                .show()
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

    private fun onTokenInfoError(throwable: Throwable) {
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
