package pm.gnosis.heimdall.ui.walletconnect

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_wallet_connect_sessions_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

@ForView
class WalletConnectSessionsAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val viewModel: WalletConnectSessionsContract,
    private val picasso: Picasso
) : LifecycleAdapter<BridgeRepository.SessionMeta, WalletConnectSessionsAdapter.SessionViewHolder>(context) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder =
        SessionViewHolder(
            viewModel,
            picasso,
            LayoutInflater.from(parent.context).inflate(R.layout.layout_wallet_connect_sessions_item, parent, false)
        )

    class SessionViewHolder(private val viewModel: WalletConnectSessionsContract, private val picasso: Picasso, itemView: View) :
        LifecycleAdapter.LifecycleViewHolder<BridgeRepository.SessionMeta>(itemView) {
        private val disposables = CompositeDisposable()
        private var current: BridgeRepository.SessionMeta? = null

        override fun bind(data: BridgeRepository.SessionMeta, payloads: List<Any>) {
            current = data
            updateSessionInfo(data)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            disposables.clear()
            current?.let { meta ->
                disposables += viewModel.observeSession(meta.id)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = ::handleSessionEvent, onError = ::handleLoadingError)

                disposables += itemView.layout_wallet_connect_sessions_item_activate.clicks()
                    .switchMapCompletable { viewModel.activateSession(meta.id) }
                    .subscribeBy(onError = Timber::e)

                disposables += itemView.layout_wallet_connect_sessions_item_kill.clicks()
                    .switchMapCompletable { viewModel.killSession(meta.id) }
                    .subscribeBy(onError = Timber::e)

                disposables += itemView.layout_wallet_connect_sessions_item_approve.clicks()
                    .switchMapCompletable { viewModel.approveSession(meta.id) }
                    .subscribeBy(onError = Timber::e)

                disposables += itemView.layout_wallet_connect_sessions_item_deny.clicks()
                    .switchMapCompletable { viewModel.denySession(meta.id) }
                    .subscribeBy(onError = Timber::e)
            }
        }

        private fun handleSessionEvent(event: BridgeRepository.SessionEvent) {
            when (event) {
                is BridgeRepository.SessionEvent.MetaUpdate -> {
                    updateSessionInfo(event.meta)
                }
                is BridgeRepository.SessionEvent.SessionRequest -> {
                    updateSessionInfo(event.meta)
                }
                is BridgeRepository.SessionEvent.Closed -> {
                } // TODO: disable interaction, for now not important as the element will be removed
                is BridgeRepository.SessionEvent.Transaction -> {
                } // noop
            }
        }

        private fun updateSessionInfo(meta: BridgeRepository.SessionMeta) {
            val hasPeerData = !meta.dappName.isNullOrBlank()
            itemView.layout_wallet_connect_sessions_item_symbol.text = if (hasPeerData) meta.dappName else "Unknown dapp"
            itemView.layout_wallet_connect_sessions_item_name.text = meta.dappDescription
            itemView.layout_wallet_connect_sessions_item_activate.visible(!meta.active)
            val unapprovedSession = hasPeerData && meta.approvedSafes?.isEmpty() != false
            itemView.layout_wallet_connect_sessions_item_kill.visible(meta.active && !unapprovedSession)
            itemView.layout_wallet_connect_sessions_item_approve.visible(meta.active && unapprovedSession)
            itemView.layout_wallet_connect_sessions_item_deny.visible(meta.active && unapprovedSession)

            meta.dappIcons?.firstOrNull()?.let {
                picasso.load(it).into(itemView.layout_wallet_connect_sessions_item_icon)
            }
        }

        private fun handleLoadingError(error: Throwable) {
            Timber.e(error)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
            disposables.clear()
        }

        override fun unbind() {
            super.unbind()
            stop()
            current = null
        }
    }
}
