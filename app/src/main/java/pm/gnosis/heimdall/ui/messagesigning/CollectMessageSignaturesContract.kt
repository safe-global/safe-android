package pm.gnosis.heimdall.ui.messagesigning

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger

abstract class CollectMessageSignaturesContract : ViewModel() {
    abstract val uiEvents: PublishSubject<UIEvent>
    abstract fun setup(payload: String, safe: Solidity.Address, threshold: Long, owners: List<Solidity.Address>, deviceSignature: Signature)
    abstract fun observe(): Observable<ViewUpdate>

    sealed class UIEvent {
        data class ViewLoaded(val deviceSignature: Signature) : UIEvent()
        object RequestSignaturesClick : UIEvent()
    }

    data class ViewUpdate(
        val payload: String,
        val signature: BigInteger?,
        val threshold: Long,
        val signaturesReceived: Long,
        val inProgress: Boolean
    )
}
