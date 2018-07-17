package pm.gnosis.heimdall.ui.safe.recover.phrase

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.model.Solidity
import javax.inject.Inject

class RecoverInputRecoveryPhraseViewModel @Inject constructor(
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper,
    private val safeRepository: GnosisSafeRepository
) : RecoverInputRecoveryPhraseContract() {
    override fun process(input: Input, safeAddress: Solidity.Address, extensionAddress: Solidity.Address): Observable<ViewUpdate> =
        recoverSafeOwnersHelper.process(input, safeAddress, extensionAddress)
            .flatMapSingle {
                when (it) {
                    is ViewUpdate.RecoverData -> {
                        // If we successfully create the recovery data add a recovering safe
                        safeRepository.addRecoveringSafe(
                            safeAddress,
                            null,
                            null,
                            it.executionInfo,
                            it.signatures
                        )
                            .andThen(Single.just<ViewUpdate>(it))
                            .onErrorReturn { ViewUpdate.RecoverDataError(it) }
                    }
                    is ViewUpdate.NoRecoveryNecessary -> {
                        safeRepository.addSafe(safeAddress)
                            .andThen(Single.just<ViewUpdate>(it))
                            .onErrorReturn { ViewUpdate.RecoverDataError(it) }
                    }
                    else -> Single.just(it)
                }
            }
}
