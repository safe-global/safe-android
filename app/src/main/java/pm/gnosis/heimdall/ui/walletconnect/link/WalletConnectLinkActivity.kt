package pm.gnosis.heimdall.ui.walletconnect.link

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.screen_wallet_connect_safe_selection.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.safe.list.SafeAdapter
import pm.gnosis.heimdall.ui.walletconnect.sessions.WalletConnectSessionsActivity
import pm.gnosis.heimdall.ui.walletconnect.sessions.WalletConnectSessionsContract
import pm.gnosis.heimdall.utils.errorSnackbar
import pm.gnosis.svalinn.common.utils.subscribeForResult
import timber.log.Timber
import javax.inject.Inject

class WalletConnectLinkActivity : ViewModelActivity<WalletConnectLinkContract>() {

    @Inject
    lateinit var adapter: SafeAdapter

    @Inject
    lateinit var layoutManager: LinearLayoutManager

    private var wcUrl: String? = null

    override fun screenId(): ScreenId = ScreenId.WALLET_CONNECT_SESSIONS

    override fun layout(): Int = R.layout.screen_wallet_connect_safe_selection

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action != Intent.ACTION_VIEW || intent.data == null) {
            finish()
            return
        }

        wcUrl = intent.data?.toString()
        wallet_connect_safe_selection_list.layoutManager = layoutManager
        wallet_connect_safe_selection_list.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        disposables += wallet_connect_safe_selection_close_btn.clicks()
            .subscribeBy { onBackPressed() }

        disposables += viewModel.observeSafes()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(onNext = adapter::updateData, onError = {
                errorSnackbar(wallet_connect_safe_selection_list, it)
            })

        disposables += adapter.safeSelection
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = Timber::e) {
                startActivity(WalletConnectSessionsActivity.createIntent(this, it.address(), wcUrl))
                finish()
            }
    }

}