package pm.gnosis.heimdall.ui.tokens.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_manage_tokens.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.postIsRefreshing
import timber.log.Timber
import javax.inject.Inject

class ManageTokensActivity : ViewModelActivity<ManageTokensContract>() {
    @Inject
    lateinit var adapter: ManageTokensAdapter

    override fun layout() = R.layout.layout_manage_tokens

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.MANAGE_TOKENS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_manage_tokens_recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        layout_manage_tokens_recycler_view.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_manage_tokens_back_arrow.clicks()
            .subscribeBy(onNext = { onBackPressed() }, onError = Timber::e)

        layout_manage_tokens_swipe_refresh.postIsRefreshing(true)
        disposables += layout_manage_tokens_swipe_refresh.refreshes()
            .startWith(Unit)
            .compose(viewModel.observeVerifiedTokens())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    adapter.updateData(it)
                    layout_manage_tokens_swipe_refresh.postIsRefreshing(false)
                },
                onError = {
                    Timber.e(it)
                    layout_manage_tokens_swipe_refresh.postIsRefreshing(false)
                    errorSnackbar(layout_manage_tokens_coordinator, it)
                })

        disposables += viewModel.observeErrors()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { errorSnackbar(layout_manage_tokens_coordinator, it) }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, ManageTokensActivity::class.java)
    }
}
