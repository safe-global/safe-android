package pm.gnosis.heimdall.ui.safe.recover

import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class ReplaceBrowserExtensionContract : ViewModel() {
    abstract fun setup(
        safeTransaction: SafeTransaction,
        signature1: Pair<Solidity.Address, Signature>,
        signature2: Pair<Solidity.Address, Signature>,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        newChromeExtension: Solidity.Address
    )

    abstract fun observeSafeBalance(): Observable<Wei>
    abstract fun submitTransaction(): Single<Result<Unit>>
    abstract fun getMaxTransactionFee(): Wei
    abstract fun getSafeTransaction(): SafeTransaction
    abstract fun loadSafe(): Single<Safe>
}
