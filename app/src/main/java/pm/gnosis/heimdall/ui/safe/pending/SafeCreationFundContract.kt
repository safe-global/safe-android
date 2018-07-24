package pm.gnosis.heimdall.ui.safe.pending

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class SafeCreationFundContract : ViewModel() {
    abstract fun setup(safeAddress: String)
    abstract fun observeCreationInfo(): Observable<Result<CreationInfo>>
    abstract fun observeHasEnoughDeployBalance(): Observable<Unit>

    data class CreationInfo(val safeAddress: String, val paymentToken: ERC20Token?, val paymentAmount: BigInteger)
}
