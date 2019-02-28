package pm.gnosis.heimdall.ui.messagesigning

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature

abstract class ReviewPayloadContract : ViewModel() {
    abstract val uiEvents: PublishSubject<UIEvent>
    abstract fun setup(payload: String, safe: Solidity.Address)
    abstract fun observe(): Observable<ViewUpdate>

    sealed class UIEvent {
        object ConfirmPayloadClick : UIEvent()
        data class SelectSafe(val safe: Solidity.Address) : UIEvent()
    }

    data class ViewUpdate(
        val payload: String,
        val isLoading: Boolean,
        val targetScreen: TargetScreen? = null,
        val safes: List<Safe> = emptyList()
    )

    sealed class TargetScreen {
        data class MessageSignaturesActivity(val safe: Solidity.Address, val payload: String, val appSignature: Signature) : TargetScreen()
    }

    object InvalidPayload : Exception()
    object ErrorSigningHash : Exception()
}
