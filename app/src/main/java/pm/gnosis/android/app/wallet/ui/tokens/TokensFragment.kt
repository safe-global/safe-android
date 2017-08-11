package pm.gnosis.android.app.wallet.ui.tokens

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_tokens.*
import pm.gnosis.android.app.wallet.R
import pm.gnosis.android.app.wallet.data.db.ERC20Token
import pm.gnosis.android.app.wallet.di.component.ApplicationComponent
import pm.gnosis.android.app.wallet.di.component.DaggerViewComponent
import pm.gnosis.android.app.wallet.di.module.ViewModule
import pm.gnosis.android.app.wallet.ui.base.BaseFragment
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
    }

    private fun onTokensList(tokens: List<ERC20Token>) {
        adapter.setItems(tokens)
        fragment_tokens_empty_view.visibility = if (tokens.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onTokensListError(throwable: Throwable) {
        Timber.e(throwable)
    }

    override fun inject(component: ApplicationComponent) {
        DaggerViewComponent.builder()
                .applicationComponent(component)
                .viewModule(ViewModule(this.context))
                .build().inject(this)
    }
}
