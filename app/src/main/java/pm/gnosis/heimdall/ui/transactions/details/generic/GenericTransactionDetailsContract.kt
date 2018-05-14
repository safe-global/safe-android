package pm.gnosis.heimdall.ui.transactions.details.generic

import android.arch.lifecycle.ViewModel
import android.content.Context
import io.reactivex.ObservableTransformer
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.svalinn.common.utils.Result

abstract class GenericTransactionDetailsContract : ViewModel() {
    abstract fun inputTransformer(context: Context, originalTransaction: SafeTransaction?): ObservableTransformer<InputEvent, Result<SafeTransaction>>

    data class InputEvent(val to: Pair<String, Boolean>, val value: Pair<String, Boolean>, val data: Pair<String, Boolean>) {
        fun diff(other: InputEvent): InputEvent =
            InputEvent(check(this.to, other.to), check(this.value, other.value), check(this.data, other.data))

        companion object {
            private fun check(current: Pair<String, Boolean>, change: Pair<String, Boolean>): Pair<String, Boolean> =
                change.first to (current.first != change.first)
        }
    }
}
