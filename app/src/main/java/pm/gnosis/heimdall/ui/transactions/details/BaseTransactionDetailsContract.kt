package pm.gnosis.heimdall.ui.transactions.details

import android.arch.lifecycle.ViewModel
import io.reactivex.Flowable
import pm.gnosis.heimdall.data.repositories.models.Safe
import java.math.BigInteger


abstract class BaseTransactionDetailsContract : ViewModel() {
    abstract fun observeSafes(defaultSafe: BigInteger?): Flowable<State>

    abstract fun updateSelectedSafe(selectedSafe: BigInteger?)

    data class State(val selectedSafeAddress: BigInteger?, val safes: List<Safe>)
}