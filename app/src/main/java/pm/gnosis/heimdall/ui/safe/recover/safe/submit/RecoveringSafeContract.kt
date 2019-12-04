package pm.gnosis.heimdall.ui.safe.recover.safe.submit

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class RecoveringSafeContract : ViewModel() {
    abstract fun checkSafeState(address: Solidity.Address): Single<Pair<RecoveringSafe, RecoveryState>>

    abstract fun checkRecoveryStatus(address: Solidity.Address): Single<Solidity.Address>

    abstract fun checkRecoveryFunded(address: Solidity.Address): Single<Solidity.Address>

    abstract fun observeRecoveryInfo(address: Solidity.Address): Observable<Result<RecoveryInfo>>

    abstract fun loadRecoveryExecuteInfo(address: Solidity.Address): Single<RecoveryExecuteInfo>

    abstract fun submitRecovery(address: Solidity.Address): Single<Solidity.Address>

    enum class RecoveryState {
        ERROR, // Parameters have changed and the recovery process needs to be restarted
        CREATED, // Transaction has been created but is not funded yet
        FUNDED, // Transaction has been created and there are enough funds to submit it
        PENDING, // Transaction has been submitted
    }

    data class RecoveryInfo(val safeAddress: String, val paymentToken: ERC20TokenWithBalance?, val paymentAmount: BigInteger, val qrCode: Bitmap?)
    data class RecoveryExecuteInfo(val balance: BigInteger, val paymentAmount: BigInteger, val paymentToken: ERC20Token, val canSubmit: Boolean)

    class TransactionExecutionException(message: String? = null) : IllegalStateException(message)

}
