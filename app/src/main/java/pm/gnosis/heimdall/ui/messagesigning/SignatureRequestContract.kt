package pm.gnosis.heimdall.ui.messagesigning

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature

abstract class SignatureRequestContract : ViewModel() {

    abstract val state: LiveData<ViewUpdate>

    abstract val viewData: ViewData

    abstract fun confirmPayload()
    abstract fun resend()
    abstract fun sign()
    abstract fun setup(payload: String, safe: Solidity.Address, signature: Signature?)

    data class ViewData(
        val safeAddress: Solidity.Address,
        val safeName: String,
        val safeBalance: String,
        val dappName: String,
        val dappAddress: Solidity.Address,
        val domainPayload: String,
        val messagePayload: String,
        val status: Status = Status.READY_TO_SIGN
    )

    data class ViewUpdate(
        val viewData: ViewData,
        val isLoading: Boolean,
        val error: Throwable? = null,
        val finishProcess: Boolean = false
    )

    enum class Status {
        AUTHORIZATION_REQUIRED,
        AUTHORIZATION_REJECTED,
        AUTHORIZATION_APPROVED,
        READY_TO_SIGN
    }

    object InvalidPayload : Exception()
    object ErrorRecoveringSender : Exception()
    object ErrorSigningHash : Exception()
    object ErrorSendingPush : Exception()
}