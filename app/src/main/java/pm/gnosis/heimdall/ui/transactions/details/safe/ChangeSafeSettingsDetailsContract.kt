package pm.gnosis.heimdall.ui.transactions.details.safe

import android.arch.lifecycle.ViewModel
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.models.Transaction
import java.math.BigInteger


abstract class ChangeSafeSettingsDetailsContract : ViewModel() {

    abstract fun loadAction(safeAddress: BigInteger?, transaction: Transaction?): Single<Action>
    abstract fun loadFormData(preset: Transaction?): Single<Pair<String, Int>>
    abstract fun inputTransformer(safeAddress: BigInteger?): ObservableTransformer<CharSequence, Result<Transaction>>

    sealed class Action {
        data class RemoveOwner(val owner: String) : Action()
        data class AddOwner(val owner: String) : Action()
        data class ReplaceOwner(val newOwner: String, val previousOwner: String) : Action()
    }
}
