package pm.gnosis.heimdall.ui.safe.details.transactions

import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.layout_safe_transactions_item.view.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.MultiSend
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.Operation
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.base.LifecycleAdapter
import pm.gnosis.heimdall.utils.displayString
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asDecimalString
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.removeHexPrefix
import timber.log.Timber
import java.math.BigInteger


abstract class BaseTransactionViewHolder<T>(
    private val addressHelper: AddressHelper,
    itemView: View
) : LifecycleAdapter.LifecycleViewHolder<T>(itemView) {

    protected val context = itemView.context

    protected val disposables = CompositeDisposable()

    protected fun updateData(safe: Solidity.Address, data: TransactionData) {
        when (data) {
            is TransactionData.Generic -> {
                val infoRes = when (data.operation) {
                    Operation.CALL -> R.string.x_data_bytes
                    Operation.DELEGATE_CALL -> R.string.x_data_bytes_delegate_call
                    Operation.CREATE -> R.string.x_data_bytes_creation
                }
                updateViews(
                    address = data.to,
                    infoText = context.getString(infoRes, (data.data?.removeHexPrefix()?.length ?: 0) / 2),
                    valueText = "- ${Wei(data.value).displayString(context)}",
                    valueColor = R.color.tomato,
                    iconRes = R.drawable.ic_transaction_outgoing
                )
            }
            is TransactionData.AssetTransfer -> {
                updateViews(
                    address = data.receiver,
                    infoText = null,
                    valueText = null,
                    valueColor = R.color.tomato,
                    iconRes = R.drawable.ic_transaction_outgoing
                )

                loadTokenInfo(data.token, data.amount)
            }
            is TransactionData.ReplaceRecoveryPhrase -> {
                updateViews(
                    address = safe,
                    infoText = context.getString(R.string.replaced_recovery_phrase),
                    valueText = null,
                    valueColor = R.color.blue,
                    iconRes = R.drawable.ic_transaction_settings
                )
            }
            is TransactionData.ConnectAuthenticator -> {
                updateViews(
                    address = safe,
                    infoText = context.getString(R.string.connect_authenticator),
                    valueText = null,
                    valueColor = R.color.blue,
                    iconRes = R.drawable.ic_transaction_settings
                )
            }
            is TransactionData.UpdateMasterCopy -> {
                updateViews(
                    address = safe,
                    infoText = context.getString(R.string.contract_upgrade),
                    valueText = null,
                    valueColor = R.color.blue,
                    iconRes = R.drawable.ic_transaction_settings
                )
            }
            is TransactionData.MultiSend -> {
                updateViews(
                    address = data.contract,
                    infoText = "${data.transactions.size} batched transaction",
                    valueText = null,
                    valueColor = R.color.blue,
                    iconRes = R.drawable.ic_transaction_outgoing
                )
            }
        }
    }

    private fun updateViews(address: Solidity.Address, infoText: String?, valueText: String?, valueColor: Int, @DrawableRes iconRes: Int) {
        itemView.layout_safe_transactions_item_type_icon.setImageResource(iconRes)
        itemView.layout_safe_transactions_item_value.text = valueText
        itemView.layout_safe_transactions_item_value.visible(valueText != null)
        itemView.layout_safe_transactions_item_value.setTextColor(context.getColorCompat(valueColor))

        itemView.layout_safe_transactions_item_info.visible(infoText != null)
        itemView.layout_safe_transactions_item_info.text = infoText

        addressHelper.populateAddressInfo(
            itemView.layout_safe_transactions_item_target_label,
            itemView.layout_safe_transactions_item_target_label,
            itemView.layout_safe_transactions_item_target_image,
            address
        ).forEach { disposables += it }
    }

    abstract fun tokenInfo(token: Solidity.Address): Single<ERC20Token>

    private fun loadTokenInfo(token: Solidity.Address, amount: BigInteger) {
        disposables += tokenInfo(token)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    itemView.layout_safe_transactions_item_value.text = "- ${it.displayString(amount)}"
                    itemView.layout_safe_transactions_item_value.visible(true)
                },
                onError = {
                    Timber.e(it)
                    itemView.layout_safe_transactions_item_value.text = "- ${amount.asDecimalString()}"
                    itemView.layout_safe_transactions_item_value.visible(true)
                }
            )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    open fun stop() {
        disposables.clear()
    }

    override fun unbind() {
        stop()
        super.unbind()
    }
}