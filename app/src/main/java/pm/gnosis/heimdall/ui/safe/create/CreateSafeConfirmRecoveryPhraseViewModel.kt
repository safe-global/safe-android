package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.model.Solidity
import javax.inject.Inject

class CreateSafeConfirmRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository
) : CreateSafeConfirmRecoveryPhraseContract() {

    private var authenticatorAddress: Solidity.Address? = null
    private var safeOwner: AccountsRepository.SafeOwner? = null

    override fun setup(authenticatorAddress: Solidity.Address?, safeOwner: AccountsRepository.SafeOwner?) {
        this.authenticatorAddress = authenticatorAddress
        this.safeOwner = safeOwner
    }

    override fun loadOwnerData(): Single<Pair<AccountsRepository.SafeOwner?, List<Solidity.Address>>> =
        accountsRepository.createOwnersFromPhrase(getRecoveryPhrase(), listOf(0, 1))
            .map { accounts ->
                safeOwner to
                        listOfNotNull(
                            authenticatorAddress,
                            accounts[0].address,
                            accounts[1].address
                        )
            }
            .subscribeOn(Schedulers.io())
}
