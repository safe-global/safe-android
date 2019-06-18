package pm.gnosis.heimdall.ui.safe.recover.safe

import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.model.Solidity
import javax.inject.Inject

class RecoverSafeRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper,
    private val safeRepository: GnosisSafeRepository
) : RecoverSafeRecoveryPhraseContract() {

    private var signingOwner: AccountsRepository.SafeOwner? = null

    override fun setup(safeOwner: AccountsRepository.SafeOwner?) {
        signingOwner = safeOwner
    }

    override fun process(input: Input, safeAddress: Solidity.Address, extensionAddress: Solidity.Address?): Observable<ViewUpdate> =
        (signingOwner?.let { Single.just(it) } ?: accountsRepository.createOwner())
            .flatMapObservable { recoverSafeOwnersHelper.process(input, safeAddress, extensionAddress, it) }
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
                            .andThen(safeRepository.saveOwner(safeAddress, it.safeOwner))
                            .andThen(Single.just<ViewUpdate>(it))
                            .onErrorReturn(ViewUpdate::RecoverDataError)
                    }
                    is ViewUpdate.NoRecoveryNecessary -> {
                        safeRepository.addSafe(safeAddress)
                            .andThen(Single.just<ViewUpdate>(it))
                            .onErrorReturn(ViewUpdate::RecoverDataError)
                    }
                    else -> Single.just(it)
                }
            }
}
