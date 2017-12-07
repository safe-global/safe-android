package pm.gnosis.heimdall.ui.transactions.details

import android.arch.lifecycle.ViewModel
import android.content.Context
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.models.Transaction
import java.math.BigInteger


abstract class AssetTransferTransactionDetailsContract : ViewModel() {
    abstract fun loadFormData(transaction: Transaction?): Single<FormData>
    abstract fun observeTokens(defaultToken: BigInteger?, safeAddress: BigInteger?): Observable<State>
    abstract fun inputTransformer(context: Context, originalTransaction: Transaction?): ObservableTransformer<CombinedRawInput, Result<Transaction>>

    data class FormData(val selectedToken: BigInteger? = null, val to: BigInteger? = null, val tokenAmount: BigInteger? = null, val token: ERC20Token? = null)

    data class State(val selectedIndex: Int, val tokens: List<ERC20TokenWithBalance>)

    data class CombinedRawInput(val to: Pair<String, Boolean>, val amount: Pair<String, Boolean>, val token: Pair<ERC20TokenWithBalance?, Boolean>) {
        fun diff(other: CombinedRawInput): CombinedRawInput =
                CombinedRawInput(check(this.to, other.to), check(this.amount, other.amount), check(this.token, other.token))

        companion object {
            private fun <T> check(current: Pair<T, Boolean>, change: Pair<T, Boolean>): Pair<T, Boolean> =
                    change.first to (current.first != change.first)
        }
    }
}