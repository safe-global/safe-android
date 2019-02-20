package pm.gnosis.heimdall.ui.walletconnect


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_wallet_connect_sessions.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.di.components.ViewComponent
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.services.BridgeService
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.base.ViewModelActivity
import pm.gnosis.heimdall.ui.qrscan.QRCodeScanActivity
import pm.gnosis.heimdall.utils.handleQrCodeActivityResult
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.lang.IllegalStateException
import javax.inject.Inject

abstract class WalletConnectSessionsContract : ViewModel() {
    abstract fun observeSessions(): Observable<Adapter.Data<BridgeRepository.SessionMeta>>
    abstract fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent>
    abstract fun createSession(url: String): Completable
    abstract fun setup(safe: Solidity.Address?)
    abstract fun activateSession(sessionId: String): Completable
    abstract fun approveSession(sessionId: String): Completable
    abstract fun denySession(sessionId: String): Completable
    abstract fun killSession(sessionId: String): Completable
}

class WalletConnectSessionsViewModel @Inject constructor(
    private val bridgeRepository: BridgeRepository
) : WalletConnectSessionsContract() {


    private var safe: Solidity.Address? = null

    override fun setup(safe: Solidity.Address?) {
        this.safe = safe
    }

    override fun observeSessions(): Observable<Adapter.Data<BridgeRepository.SessionMeta>> =
        bridgeRepository.observeSessions().scanToAdapterData(idExtractor = { it.id })

    override fun observeSession(sessionId: String): Observable<BridgeRepository.SessionEvent> =
        bridgeRepository.observeSession(sessionId)

    override fun createSession(url: String): Completable =
        Single.fromCallable {
            bridgeRepository.createSession(url)
        }.flatMapCompletable {
            bridgeRepository.initSession(it)
        }

    override fun activateSession(sessionId: String): Completable =
        bridgeRepository.activateSession(sessionId).andThen(bridgeRepository.initSession(sessionId))

    override fun approveSession(sessionId: String): Completable =
        safe?.let { bridgeRepository.approveSession(sessionId, it) } ?: Completable.error(IllegalStateException("no safe set"))

    override fun denySession(sessionId: String): Completable =
        bridgeRepository.rejectSession(sessionId)

    override fun killSession(sessionId: String): Completable =
        bridgeRepository.closeSession(sessionId)

}

class WalletConnectSessionsActivity : ViewModelActivity<WalletConnectSessionsContract>() {

    @Inject
    lateinit var adapter: WalletConnectSessionsAdapter

    override fun screenId() = ScreenId.WALLET_CONNECT_SESSIONS

    override fun layout() = R.layout.layout_wallet_connect_sessions

    override fun inject(component: ViewComponent) = component.inject(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout_wallet_connect_sessions_recycler_view.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        layout_wallet_connect_sessions_recycler_view.adapter = adapter
        viewModel.setup(intent.getStringExtra(EXTRA_SAFE)?.asEthereumAddress())
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

        disposables += viewModel.observeSessions().observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = adapter::updateData, onError = Timber::e)

        disposables += layout_wallet_connect_sessions_add.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { QRCodeScanActivity.startForResult(this) }
    }

    companion object {

        private const val EXTRA_SAFE = "argument.string.safe"
        fun createIntent(context: Context, safe: Solidity.Address?) = Intent(context, WalletConnectSessionsActivity::class.java).apply {
            putExtra(EXTRA_SAFE, safe?.asEthereumAddressString())
        }
    }
}
