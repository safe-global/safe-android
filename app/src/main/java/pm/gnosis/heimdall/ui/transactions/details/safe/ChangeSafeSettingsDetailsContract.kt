package pm.gnosis.heimdall.ui.transactions.details.safe

import android.arch.lifecycle.ViewModel
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class ChangeSafeSettingsDetailsContract : ViewModel() {
    abstract fun loadAction(safeAddress: Solidity.Address?, transaction: Transaction?): Single<Action>
    abstract fun loadFormData(preset: Transaction?): Single<Pair<String, Int>>
    abstract fun inputTransformer(safeAddress: Solidity.Address?): ObservableTransformer<CharSequence, Result<Transaction>>

    sealed class Action {
        data class RemoveOwner(val owner: Solidity.Address) : Action()
        data class AddOwner(val owner: Solidity.Address) : Action()
        data class ReplaceOwner(val newOwner: Solidity.Address, val previousOwner: Solidity.Address) : Action()
    }
}
