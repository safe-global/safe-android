package pm.gnosis.heimdall.ui.safe.selection

import android.arch.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Single
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.models.Transaction
import java.math.BigInteger


abstract class SelectSafeContract: ViewModel() {
    abstract fun loadSafes(): Single<List<Safe>>
    abstract fun reviewTransaction(safe: BigInteger?, transaction: Transaction): Single<Result<Intent>>
}
