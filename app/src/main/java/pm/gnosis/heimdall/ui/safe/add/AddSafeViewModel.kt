package pm.gnosis.heimdall.ui.safe.add

import io.reactivex.Observable
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.models.Wei
import pm.gnosis.utils.hexAsEthereumAddress
import javax.inject.Inject


class AddSafeViewModel @Inject constructor(
        private val repository: GnosisSafeRepository
): AddSafeContract() {
    override fun addExistingSafe(name: String, address: String): Observable<Result<Unit>> {
        return repository.add(address.hexAsEthereumAddress(), name)
                .andThen(Observable.just(Unit))
                .mapToResult()
    }

    override fun deployNewSafe(name: String): Observable<Result<Unit>> {
        // Current device will be added by default for now
        return repository.deploy(name, emptySet(), 1)
                .andThen(Observable.just(Unit))
                .mapToResult()
    }

    override fun observeEstimate(): Observable<Wei> {
        return repository.estimateDeployCosts(emptySet(), 1).toObservable()
    }
}