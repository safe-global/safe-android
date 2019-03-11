package pm.gnosis.heimdall.ui.messagesigning

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature

abstract class ConfirmMessageContract : ViewModel() {
    abstract val uiEvents: PublishSubject<UIEvent>
    abstract fun setup(payload: String, safe: Solidity.Address, signature: Signature)
    abstract fun observe(): Observable<ViewUpdate>

    sealed class UIEvent {
        object ConfirmPayloadClick : UIEvent()
    }

    data class ViewUpdate(
        val payload: String,
        val isLoading: Boolean,
        val error: Throwable? = null,
        val finishProcess: Boolean = false
    )

    object InvalidPayload : Exception()
    object ErrorRecoveringSender : Exception()
    object ErrorSigningHash : Exception()
    object ErrorSendingPush : Exception()
}
