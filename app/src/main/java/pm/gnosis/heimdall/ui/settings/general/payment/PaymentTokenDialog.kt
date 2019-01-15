package pm.gnosis.heimdall.ui.settings.general.payment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_payment_token.*
import kotlinx.android.synthetic.main.dialog_payment_token.view.*
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.di.components.DaggerViewComponent
import pm.gnosis.heimdall.di.modules.ViewModule
import pm.gnosis.heimdall.reporting.Event
import pm.gnosis.heimdall.reporting.EventTracker
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.dialogs.base.BaseDialog
import pm.gnosis.heimdall.utils.CustomAlertDialogBuilder
import pm.gnosis.svalinn.common.utils.subscribeForResult
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import javax.inject.Inject

class PaymentTokenDialog : BaseDialog() {

    @Inject
    lateinit var adapter: PaymentTokensAdapter

    @Inject
    lateinit var viewModel: PaymentTokenContract

    @Inject
    lateinit var eventTracker: EventTracker

    var successListener: ((ERC20Token) -> Unit)? = null

    var errorListener: ((Throwable) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setStyle(DialogFragment.STYLE_NO_FRAME, 0)
        super.onCreate(savedInstanceState)
        inject()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_payment_token, null)
        dialogView.dialog_payment_token_list.layoutManager = LinearLayoutManager(context)
        dialogView.dialog_payment_token_list.adapter = adapter
        return CustomAlertDialogBuilder.build(context!!, getString(R.string.select_payment_token), dialogView, 0, null, 0, null)
    }

    override fun onStart() {
        super.onStart()
        eventTracker.submit(Event.ScreenView(ScreenId.SELECT_PAYMENT_TOKEN))

        disposables += viewModel.loadPaymentTokens()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = ::onPaymentTokensLoaded,
                onError = ::onPaymentTokensError
            )

        disposables += adapter.tokenSelectedSubject
            .switchMapSingle { viewModel.setPaymentToken(it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeForResult(
                onNext = {
                    successListener?.invoke(it)
                    dismiss()
                }, onError = Timber::e
            )
    }

    private fun onPaymentTokensLoaded(tokens: Adapter.Data<ERC20Token>) {
        adapter.updateData(tokens)
        dialog.dialog_payment_token_loading.visible(false)
    }

    private fun onPaymentTokensError(throwable: Throwable) {
        Timber.e(throwable)
        errorListener?.invoke(throwable)
        dismiss()
    }

    private fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!])
            .build()
            .inject(this)
    }

    companion object {
        fun create() = PaymentTokenDialog()
    }
}
