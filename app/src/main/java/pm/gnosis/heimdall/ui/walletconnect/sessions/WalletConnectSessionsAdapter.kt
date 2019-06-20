package pm.gnosis.heimdall.ui.walletconnect.sessions

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxbinding2.view.clicks
import com.squareup.picasso.Picasso
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_content_simple_text.view.*
import kotlinx.android.synthetic.main.layout_adapter_entry_header.view.*
import kotlinx.android.synthetic.main.layout_wallet_connect_sessions_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.ui.base.LifecycleAdapter.LifecycleViewHolder
import pm.gnosis.heimdall.ui.walletconnect.sessions.WalletConnectSessionsContract.AdapterEntry
import pm.gnosis.heimdall.utils.CustomAlertDialogBuilder
import pm.gnosis.heimdall.utils.SwipeableTouchHelperCallback
import pm.gnosis.heimdall.utils.SwipeableViewHolder
import pm.gnosis.heimdall.utils.errorToast
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

@ForView
class WalletConnectSessionsAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val viewModel: WalletConnectSessionsContract,
    private val picasso: Picasso
) : LifecycleAdapter<AdapterEntry, LifecycleViewHolder<AdapterEntry>>(context) {

    private val touchCallback by lazy { SwipeableTouchHelperCallback() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LifecycleViewHolder<AdapterEntry> =
        when (viewType) {
            R.id.adapter_entry_header ->
                HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_adapter_entry_header, parent, false))
            R.id.adapter_entry_session ->
                SessionViewHolder(
                    viewModel,
                    picasso,
                    { notifyItemChanged(it.adapterPosition) },
                    LayoutInflater.from(parent.context).inflate(R.layout.layout_wallet_connect_sessions_item, parent, false)
                )
            else -> throw IllegalArgumentException("Unknown type")
        }

    override fun getItemViewType(position: Int): Int {
        return items.getOrNull(position)?.type ?: 0
    }

    fun attach(recyclerView: RecyclerView) {
        recyclerView.adapter = this
        ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)
    }

    class HeaderViewHolder(itemView: View) : LifecycleViewHolder<AdapterEntry>(itemView) {
        override fun bind(data: AdapterEntry, payloads: List<Any>) {
            (data as? AdapterEntry.Header)?.apply {
                itemView.layout_adapter_entry_header_title.text = title
            }
        }
    }

    class SessionViewHolder(
        private val viewModel: WalletConnectSessionsContract,
        private val picasso: Picasso,
        private val swipeCanceledCallback: (SessionViewHolder) -> Unit,
        itemView: View
    ) :
        LifecycleViewHolder<AdapterEntry>(itemView), SwipeableViewHolder {
        private val disposables = CompositeDisposable()
        private var current: BridgeRepository.SessionMeta? = null

        override fun bind(data: AdapterEntry, payloads: List<Any>) {
            (data as? AdapterEntry.Session)?.apply {
                current = meta
                updateSessionInfo(meta)
            }
        }

        override fun onSwiped() {
            val meta = current ?: return
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_content_simple_text, null)
            val dappName = if (meta.dappName.isNullOrBlank()) meta.dappName else itemView.context.getString(R.string.unknown_dapp)
            dialogView.dialog_content_simple_text_content.text = context.getString(R.string.this_will_disconnect_x, dappName)
            CustomAlertDialogBuilder.build(
                context,
                context.getString(R.string.disconnect_from_dapp),
                dialogView,
                confirmRes = R.string.disconnect,
                confirmCallback = {
                    closeSession()
                    it.dismiss()
                },
                cancelCallback = {
                    it.dismiss()
                },
                dismissCallback = DialogInterface.OnDismissListener {
                    cancelSwipe()
                }

            ).show()
        }

        private fun closeSession() {
            current?.let { meta ->
                viewModel.killSession(meta.id).subscribeBy(onError = {
                    Timber.e(it)
                    itemView.context.errorToast(it)
                })
            }
        }

        private fun cancelSwipe() {
            swipeCanceledCallback(this)
        }

        override fun swipeableView(): View =
            itemView.layout_wallet_connect_sessions_item_foreground

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
            }
        }

        private fun handleSessionEvent(event: BridgeRepository.SessionEvent) {
            when (event) {
                is BridgeRepository.SessionEvent.MetaUpdate -> {
                    updateSessionInfo(event.meta)
                }
                is BridgeRepository.SessionEvent.Closed -> {
                } // TODO: disable interaction, for now not important as the element will be removed
                is BridgeRepository.SessionEvent.Transaction -> {
                } // noop
            }
        }

        private fun updateSessionInfo(meta: BridgeRepository.SessionMeta) {
            if (meta.dappName == null) {
                itemView.layout_wallet_connect_sessions_item_title.setText(R.string.please_wait)
                itemView.layout_wallet_connect_sessions_item_subtitle.setText(R.string.connecting)
            } else {
                itemView.layout_wallet_connect_sessions_item_title.text =
                    if (meta.dappName.isNotBlank()) meta.dappName else itemView.context.getString(R.string.unknown_dapp)
                itemView.layout_wallet_connect_sessions_item_subtitle.text = meta.dappUrl ?: meta.dappDescription
            }
            itemView.layout_wallet_connect_sessions_item_activate.visible(!meta.active)

            itemView.layout_wallet_connect_sessions_item_icon.setImageResource(R.drawable.image_placeholder)
            meta.dappIcons?.firstOrNull()?.let {
                itemView.layout_wallet_connect_sessions_item_image_text.text = null
                picasso.load(it).into(itemView.layout_wallet_connect_sessions_item_icon)
            } ?: run {
                itemView.layout_wallet_connect_sessions_item_image_text.visible(true)
                itemView.layout_wallet_connect_sessions_item_image_text.text = meta.dappName?.first()?.toUpperCase()?.toString()
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
