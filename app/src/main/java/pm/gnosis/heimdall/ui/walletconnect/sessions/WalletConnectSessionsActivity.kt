package pm.gnosis.heimdall.ui.walletconnect.sessions


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_wallet_connect_sessions.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class WalletConnectSessionsActivity : ViewModelActivity<WalletConnectSessionsContract>() {

    @Inject
    lateinit var adapter: WalletConnectSessionsAdapter

    private lateinit var safe: Solidity.Address

    override fun screenId() = ScreenId.WALLET_CONNECT_SESSIONS

    override fun layout() = R.layout.layout_wallet_connect_sessions

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        safe = intent.getStringExtra(EXTRA_SAFE_ADDRESS)?.asEthereumAddress() ?: run { finish(); return }
        viewModel.setup(safe)
        layout_wallet_connect_sessions_recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        adapter.attach(layout_wallet_connect_sessions_recycler_view)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!handleQrCodeActivityResult(requestCode, resultCode, data, {
                disposables += viewModel.createSession(it).subscribeBy(onError = Timber::e)
            })) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += layout_wallet_connect_sessions_back_arrow.clicks()
            .subscribeBy { onBackPressed() }

        disposables += viewModel.observeSessions()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { layout_wallet_connect_sessions_empty_view.visible(it.entries.isEmpty()) }
            .subscribeBy(onNext = adapter::updateData, onError = Timber::e)

        disposables += layout_wallet_connect_sessions_add.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { QRCodeScanActivity.startForResult(this) }

        intent.getStringExtra(EXTRA_WC_URI)?.let {
            intent.removeExtra(EXTRA_WC_URI)
            disposables += viewModel.createSession(it).subscribeBy(onError = Timber::e)
        }
    }

    companion object {
        private const val EXTRA_WC_URI = "extra.string.wc_uri"
        private const val EXTRA_SAFE_ADDRESS = "extra.string.safe_address"
        fun createIntent(context: Context, safe: Solidity.Address, wcUri: String? = null) =
            Intent(context, WalletConnectSessionsActivity::class.java).apply {
                putExtra(EXTRA_SAFE_ADDRESS, safe.asEthereumAddressString())
                putExtra(EXTRA_WC_URI, wcUri)
            }
    }
}
