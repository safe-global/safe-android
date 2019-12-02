package pm.gnosis.heimdall.ui.transactions.view.helpers

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.include_transaction_submit_info.view.*
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.two_factor.keycard.KeycardSigningDialog
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.svalinn.common.utils.getColorCompat
import pm.gnosis.svalinn.common.utils.visible
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TransactionSubmitInfoViewHelper @Inject constructor() {

    var onToggleReadyState: ((Boolean) -> Unit)? = null

    private lateinit var view: View
    private lateinit var context: Context

    fun bind(container: View) {
        view = container
        context = view.context
    }

    fun resetConfirmationViews() {
        view.include_transaction_submit_info_keycard_group.visible(false)
        view.include_transaction_submit_info_confirmations_group.visible(false)
        view.include_transaction_submit_info_request_button.isEnabled = false
        view.include_transaction_submit_info_confirmations_timer.text = context.getString(R.string.request_confirmation_wait_x_s, 30.toString())
    }

    fun retryEvents(): Observable<Unit> =
        view.include_transaction_submit_info_retry_button.clicks()
            .doOnNext {
                view.include_transaction_submit_info_confirmation_progress.visible(true)
                view.include_transaction_submit_info_retry_button.visible(false)
            }

    fun requestConfirmationEvents(): Observable<Unit> =
        view.include_transaction_submit_info_request_button.clicks()
            .doOnNext {
                toggleRejectionState(false)
                view.include_transaction_submit_info_request_button.isEnabled = false
                view.include_transaction_submit_info_confirmations_timer.text =
                    context.getString(R.string.request_confirmation_wait_x_s, 30.toString())
            }

    fun toggleRejectionState(rejected: Boolean) {
        view.include_transaction_submit_info_confirmation_progress.apply {
            isIndeterminate = !rejected
            max = 100
            progress = 100
            progressTintList =
                ColorStateList.valueOf(context.getColorCompat(if (rejected) R.color.tomato_15 else R.color.safe_green))
            progressTintMode = PorterDuff.Mode.SRC_IN
        }

        view.include_transaction_submit_info_confirmations_image.setImageResource(
            if (rejected) R.drawable.ic_rejected_extension else R.drawable.ic_confirmations_waiting
        )
        view.include_transaction_submit_info_confirmations_info.setTextColor(
            context.getColorCompat(if (rejected) R.color.tomato else R.color.blue)
        )
        view.include_transaction_submit_info_confirmations_info.text =
            context.getString(if (rejected) R.string.rejected_by_extension else R.string.confirm_with_extension)
    }

    fun toggleReadyState(isReady: Boolean, inProgressMessage: Int = R.string.awaiting_confirmations) {
        view.include_transaction_submit_info_confirmation_status.text = context.getString(
            if (isReady) R.string.confirmations_ready
            else inProgressMessage
        )
        view.include_transaction_submit_info_confirmation_progress.apply {
            isIndeterminate = !isReady
            max = 100
            progress = 100
        }
        onToggleReadyState?.invoke(isReady)
    }

    fun setupViewHolder(layoutInflater: LayoutInflater, viewHolder: TransactionInfoViewHolder) {
        // Remove previous views (normally the progress bar)
        view.include_transaction_submit_info_data_container.removeAllViews()
        // Add view holder views
        viewHolder.inflate(layoutInflater, view.include_transaction_submit_info_data_container)
    }

    fun applyUpdate(update: SubmitTransactionHelper.ViewUpdate): Disposable? {
        when (update) {
            is SubmitTransactionHelper.ViewUpdate.Estimate -> {
                val balanceColor = context.getColorCompat(if (update.sufficientFunds) R.color.dark_grey else R.color.tomato)
                update.assetBalanceAfterTransfer?.let {
                    view.include_transaction_submit_info_data_asset_balance_label.visible(true)
                    view.include_transaction_submit_info_data_asset_balance_value.visible(true)
                    view.include_transaction_submit_info_data_asset_balance_value.text = update.assetBalanceAfterTransfer.displayString()
                } ?: run {
                    view.include_transaction_submit_info_data_asset_balance_label.visible(false)
                    view.include_transaction_submit_info_data_asset_balance_value.visible(false)
                }
                view.include_transaction_submit_info_data_gas_token_balance_label.setTextColor(balanceColor)
                view.include_transaction_submit_info_data_gas_token_balance_value.setTextColor(balanceColor)
                view.include_transaction_submit_info_data_gas_token_balance_value.text = update.gasToken.displayString()
                view.include_transaction_submit_info_data_fees_value.text = update.gasToken.token.displayString(update.networkFee)
                view.include_transaction_submit_info_data_fees_error.visible(!update.sufficientFunds)
                view.include_transaction_submit_info_retry_button.visible(!update.sufficientFunds)
            }
            is SubmitTransactionHelper.ViewUpdate.EstimateError -> {
                view.include_transaction_submit_info_confirmation_progress.visible(false)
                view.include_transaction_submit_info_retry_button.visible(true)
            }
            is SubmitTransactionHelper.ViewUpdate.RequireConfirmations -> {
                when (update.authenticatorInfo.type) {
                    AuthenticatorInfo.Type.KEYCARD -> {
                        view.include_transaction_submit_info_confirmations_group.visible(false)
                        view.include_transaction_submit_info_keycard_group.visible(true)
                        view.include_transaction_submit_info_keycard_button.setOnClickListener {
                            (context as? AppCompatActivity)?.apply {
                                KeycardSigningDialog.create(update.authenticatorInfo.address, update.hash).show(supportFragmentManager, null)
                            }
                        }
                    }
                    AuthenticatorInfo.Type.EXTENSION -> {
                        view.include_transaction_submit_info_confirmations_group.visible(true)
                        view.include_transaction_submit_info_keycard_group.visible(false)
                        view.include_transaction_submit_info_keycard_button.setOnClickListener(null)
                    }
                }
            }
            is SubmitTransactionHelper.ViewUpdate.Confirmations -> {
                toggleReadyState(update.isReady)
                if (update.isReady) {
                    view.include_transaction_submit_info_confirmations_group.visible(false)
                    view.include_transaction_submit_info_keycard_group.visible(false)
                }
            }
            is SubmitTransactionHelper.ViewUpdate.ConfirmationsRequested -> {
                return Observable.interval(1, TimeUnit.SECONDS).take(30)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(onNext = {
                        view.include_transaction_submit_info_confirmations_timer.text =
                            context.getString(R.string.request_confirmation_wait_x_s, (30 - it - 1).toString())
                    }, onComplete = {
                        view.include_transaction_submit_info_request_button.isEnabled = true
                        view.include_transaction_submit_info_confirmations_timer.text = null
                    })
            }
            is SubmitTransactionHelper.ViewUpdate.ConfirmationsError -> {
                view.include_transaction_submit_info_request_button.isEnabled = true
                view.include_transaction_submit_info_confirmations_timer.text = null
            }
            SubmitTransactionHelper.ViewUpdate.TransactionRejected -> {
                toggleRejectionState(true)
            }
            else -> throw IllegalStateException("Unhandled update $update")
        }
        return null
    }
}
