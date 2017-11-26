package pm.gnosis.heimdall.ui.safe.add

import android.content.Context
import io.reactivex.Observable
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.models.Wei
import pm.gnosis.utils.hexAsEthereumAddress
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import javax.inject.Inject


class AddSafeViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val repository: GnosisSafeRepository
) : AddSafeContract() {
    override fun addExistingSafe(name: String, address: String): Observable<Result<Unit>> {
        return Observable.fromCallable {
            checkName(name)
            val parsedAddress = address.hexAsEthereumAddressOrNull() ?: throw LocalizedException(context.getString(R.string.invalid_ethereum_address))
            parsedAddress to name
        }.flatMap { (address, name) ->
            repository.add(address, name)
                    .andThen(Observable.just(Unit))
        }
                .mapToResult()
    }

    override fun deployNewSafe(name: String): Observable<Result<Unit>> {
        return Observable.fromCallable {
            checkName(name)
            name
        }.flatMap {
            // Current device will be added by default for now
            repository.deploy(name, emptySet(), 1)
                    .andThen(Observable.just(Unit))
        }
                .mapToResult()
    }

    override fun observeEstimate(): Observable<Wei> {
        return repository.estimateDeployCosts(emptySet(), 1).toObservable()
    }

    private fun checkName(name: String) {
        if (name.isBlank()) throw LocalizedException(context.getString(R.string.error_blank_name))
    }
}