package pm.gnosis.heimdall.ui.safe.recover.extension

import androidx.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class ReplaceExtensionSubmitContract : ViewModel() {
    abstract fun setup(
        safeTransaction: SafeTransaction,
        signature1: Signature,
        signature2: Signature,
        txGas: BigInteger,
        dataGas: BigInteger,
        operationalGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        newChromeExtension: Solidity.Address,
        txHash: ByteArray
    )

    abstract fun observeSubmitStatus(): Observable<Result<SubmitStatus>>
    abstract fun submitTransaction(): Single<Result<Unit>>
    abstract fun loadFeeInfo(): Single<ERC20TokenWithBalance>
    abstract fun getSafeTransaction(): SafeTransaction
    abstract fun loadSafe(): Single<Safe>

    data class SubmitStatus(val balance: ERC20TokenWithBalance, val balanceAfter: ERC20TokenWithBalance, val canSubmit: Boolean)

    class NoTokenBalanceException : Exception()
}
