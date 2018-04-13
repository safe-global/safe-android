package pm.gnosis.heimdall.ui.safe.add

import android.arch.lifecycle.ViewModel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.ticker.data.repositories.models.Currency
import java.math.BigDecimal

abstract class AddSafeContract : ViewModel() {
    abstract fun addExistingSafe(name: String, address: String): Single<Result<Solidity.Address>>

    abstract fun deployNewSafe(name: String): Single<Result<String>>

    abstract fun saveTransactionHash(transactionHash: String, name: String): Completable

    abstract fun loadFiatConversion(wei: Wei): Single<Result<Pair<BigDecimal, Currency>>>

    abstract fun addAdditionalOwner(input: String): Observable<Result<Unit>>

    abstract fun removeAdditionalOwner(address: Solidity.Address): Observable<Result<Unit>>

    abstract fun setupDeploy(): Single<Solidity.Address>

    abstract fun estimateDeploy(): Single<Result<FeeEstimate>>

    abstract fun observeAdditionalOwners(): Observable<List<Solidity.Address>>

    abstract fun loadSafeInfo(address: String): Observable<Result<SafeInfo>>

    abstract fun loadActiveAccount(): Observable<Account>

    abstract fun loadDeployData(name: String): Single<Result<Transaction>>
}
