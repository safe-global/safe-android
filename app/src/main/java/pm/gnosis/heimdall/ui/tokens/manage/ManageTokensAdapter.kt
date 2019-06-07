package pm.gnosis.heimdall.ui.tokens.manage

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.jakewharton.rxbinding2.widget.checkedChanges
import com.squareup.picasso.Picasso
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_manage_tokens_item.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ForView
import pm.gnosis.heimdall.di.ViewContext
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import timber.log.Timber
import javax.inject.Inject

@ForView
class ManageTokensAdapter @Inject constructor(
    @ViewContext private val context: Context,
    private val viewModel: ManageTokensContract,
    private val picasso: Picasso
) : LifecycleAdapter<ManageTokensContract.ERC20TokenEnabled, ManageTokensAdapter.TokenViewHolder>(context) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder =
        TokenViewHolder(viewModel, picasso, LayoutInflater.from(parent.context).inflate(R.layout.layout_manage_tokens_item, parent, false))

    class TokenViewHolder(private val viewModel: ManageTokensContract, private val picasso: Picasso, itemView: View) :
        LifecycleAdapter.LifecycleViewHolder<ManageTokensContract.ERC20TokenEnabled>(itemView) {
        private val disposables = CompositeDisposable()
        private var current: ManageTokensContract.ERC20TokenEnabled? = null

        override fun bind(data: ManageTokensContract.ERC20TokenEnabled, payloads: List<Any>) {
            itemView.layout_manage_tokens_symbol.text = data.erc20Token.symbol
            itemView.layout_manage_tokens_name.text = data.erc20Token.name
            itemView.layout_manage_tokens_switch.isChecked = data.enabled

            picasso.load(data.erc20Token.logoUrl).into(itemView.layout_manage_tokens_icon)
            current = data
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun start() {
            disposables.clear()
            current?.let {
                disposables += itemView.layout_manage_tokens_switch.checkedChanges()
                    .skipInitialValue()
                    .switchMapSingle { enabled ->
                        if (enabled) viewModel.enableToken(it.erc20Token)
                        else viewModel.disableToken(it.erc20Token.address)
                    }
                    .subscribeBy(onError = Timber::e)
            }
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
