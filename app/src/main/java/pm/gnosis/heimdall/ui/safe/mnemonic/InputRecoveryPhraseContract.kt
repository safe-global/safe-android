package pm.gnosis.heimdall.ui.safe.mnemonic

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature

abstract class InputRecoveryPhraseContract : ViewModel() {

    abstract fun process(input: Input, safeAddress: Solidity.Address, authenticatorInfo: AuthenticatorSetupInfo?): Observable<ViewUpdate>

    data class Input(
        val phrase: Observable<CharSequence>,
        val retry: Observable<Unit>,
        val create: Observable<Unit>
    )

    sealed class ViewUpdate {

        data class SafeInfoError(val error: Throwable) : ViewUpdate()

        object InputMnemonic : ViewUpdate()

        object InvalidMnemonic : ViewUpdate()

        object WrongMnemonic : ViewUpdate()

        object ValidMnemonic : ViewUpdate()

        data class NoRecoveryNecessary(
            val safeAddress: Solidity.Address
        ) : ViewUpdate()

        data class RecoverDataError(val error: Throwable) : ViewUpdate()

        data class RecoverData(
            val executionInfo: TransactionExecutionRepository.ExecuteInformation,
            val signatures: List<Signature>,
            val safeOwner: AccountsRepository.SafeOwner
        ) : ViewUpdate()
    }
}
