package pm.gnosis.heimdall.ui.safe.pending

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import java.math.BigInteger

abstract class PendingSafeContract : ViewModel() {
    abstract fun setup(transactionHash: String)
    abstract fun observePendingSafe(): Observable<PendingSafe>
    abstract fun observeHasEnoughDeployBalance(): Observable<Unit>
    abstract fun getTransactionHash(): BigInteger?
}
