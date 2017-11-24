package pm.gnosis.heimdall.ui.settings.tokens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_token_management.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.components.DaggerViewComponent
import pm.gnosis.heimdall.common.di.modules.ViewModule
import pm.gnosis.heimdall.common.utils.snackbar
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.BaseActivity
import pm.gnosis.heimdall.ui.tokens.add.AddTokenActivity
import timber.log.Timber
import javax.inject.Inject

class TokenManagementActivity : BaseActivity() {
    @Inject
    lateinit var viewModel: TokenManagementContract

    @Inject
    lateinit var adapter: TokenManagementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inject()
        setContentView(R.layout.layout_token_management)
        registerToolbar(layout_token_management_toolbar)
        layout_token_management_toolbar.title = "Manage Verified Tokens"

        layout_token_management_fab.setOnClickListener {
            startActivity(AddTokenActivity.createIntent(this))
        }

        val layoutManager = LinearLayoutManager(this)
        layout_token_management_list.layoutManager = layoutManager
        layout_token_management_list.adapter = this.adapter
    }

    override fun onStart() {
        super.onStart()
        disposables += viewModel.observeVerifiedTokens()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = this::onTokensList, onError = this::onTokensListError)
    }

    private fun onTokensList(tokens: Adapter.Data<ERC20Token>) {
        adapter.updateData(tokens)
    }

    private fun onTokensListError(throwable: Throwable) {
        Timber.e(throwable)
        snackbar(layout_token_management_coordinator, R.string.tokens_list_error)
    }

    fun inject() {
        DaggerViewComponent.builder()
                .applicationComponent(HeimdallApplication[this].component)
                .viewModule(ViewModule(this))
                .build()
                .inject(this)
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, TokenManagementActivity::class.java)
    }
}
