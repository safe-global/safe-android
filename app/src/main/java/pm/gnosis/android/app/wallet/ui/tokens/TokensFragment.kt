package pm.gnosis.android.app.wallet.ui.tokens

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_token_info.view.*
import kotlinx.android.synthetic.main.fragment_tokens.*
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.db.ERC20Token
import pm.gnosis.android.app.wallet.di.component.ApplicationComponent
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.base.BaseFragment
import pm.gnosis.android.app.wallet.util.ERC20
import pm.gnosis.android.app.wallet.util.asDecimalString
import pm.gnosis.android.app.wallet.util.snackbar
import timber.log.Timber
import javax.inject.Inject

class TokensFragment : BaseFragment() {
    @Inject lateinit var presenter: TokensPresenter
    @Inject lateinit var adapter: TokensAdapter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater?.inflate(R.layout.fragment_tokens, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragment_tokens_list.layoutManager = LinearLayoutManager(context)
        fragment_tokens_list.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        disposables += presenter.observeTokens()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onTokensList, onError = this::onTokensListError)

        disposables += adapter.tokensSelection
                .flatMap {
                    presenter.observeTokenInfo(it)
                            ?.observeOn(AndroidSchedulers.mainThread())
                            ?.doOnSubscribe { onTokenInfoLoading(true) }
                            ?.doOnTerminate { onTokenInfoLoading(false) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onTokenInfo, onError = this::onTokenInfoError)
    }

    private fun onTokensList(tokens: List<ERC20Token>) {
        adapter.setItems(tokens)
        fragment_tokens_empty_view.visibility = if (tokens.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onTokensListError(throwable: Throwable) {
        Timber.e(throwable)
    }

    private fun onTokenInfo(token: ERC20.Token) {
        if (token.decimals == null && token.name == null && token.symbol == null) {
            snackbar(fragment_tokens_coordinator_layout, "Could not get token information")
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_token_info, null)

        if (token.decimals != null) {
            dialogView.dialog_token_info_decimals.text = token.decimals.asDecimalString()
        } else {
            dialogView.dialog_token_info_decimals_container.visibility = View.GONE
        }

        dialogView.dialog_token_info_address.text = token.address
        dialogView.dialog_token_info_symbol.text = token.symbol

        AlertDialog.Builder(context)
                .setView(dialogView)
                .setTitle(token.name)
                .show()
    }

    private fun onTokenInfoLoading(isLoading: Boolean) {
        adapter.itemsClickable = !isLoading
        fragment_tokens_progress_bar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun onTokenInfoError(throwable: Throwable) {
        Timber.e(throwable)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(this.context))
                .build().inject(this)
    }
}
