package pm.gnosis.heimdall.ui.tokens.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.support.v4.widget.refreshes
import com.jakewharton.rxbinding2.view.clicks
import com.jakewharton.rxbinding2.widget.textChanges
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_manage_tokens.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.heimdall.utils.postIsRefreshing
import pm.gnosis.svalinn.common.utils.showKeyboardForView
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ManageTokensActivity : ViewModelActivity<ManageTokensContract>() {
    @Inject
    lateinit var adapter: ManageTokensAdapter

    override fun layout() = R.layout.layout_manage_tokens

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun screenId() = ScreenId.MANAGE_TOKENS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_manage_tokens_recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        layout_manage_tokens_recycler_view.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_manage_tokens_search_clear_btn.clicks()
            .subscribeBy(onNext = { layout_manage_tokens_search_input.text = null }, onError = Timber::e)

        disposables += layout_manage_tokens_search_btn.clicks()
            .subscribeBy(onNext = { toggleSearch(true) }, onError = Timber::e)

        disposables += layout_manage_tokens_back_arrow.clicks()
            .subscribeBy(onNext = {
                if (layout_manage_tokens_search_input.visibility == View.VISIBLE) {
                    onBackPressed()
                } else {
                    toggleSearch(false)
                }
            }, onError = Timber::e)

        layout_manage_tokens_swipe_refresh.postIsRefreshing(true)
        disposables += Observable.combineLatest(
            layout_manage_tokens_swipe_refresh.refreshes().startWith(Unit),
            layout_manage_tokens_search_input.textChanges()
                .doOnNext { layout_manage_tokens_search_clear_btn.visible(it.isNotEmpty()) }
                .debounce(200, TimeUnit.MILLISECONDS),
            BiFunction<Unit, CharSequence, String> { _, input -> input.toString() }
        )
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                layout_manage_tokens_swipe_refresh.postIsRefreshing(true)
            }
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

    private fun toggleSearch(enabled: Boolean) {
        layout_manage_tokens_title_group.visible(!enabled)
        layout_manage_tokens_search_input.visible(enabled)
        if (!enabled) {
            layout_manage_tokens_search_input.text = null
            layout_manage_tokens_search_clear_btn.visible(false)
        } else {
            layout_manage_tokens_search_input.showKeyboardForView()
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, ManageTokensActivity::class.java)
    }
}
