package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import javax.inject.Inject

class CreateSafeConfirmRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository
) : CreateSafeConfirmRecoveryPhraseContract() {

    private var authenticatorInfo: AuthenticatorSetupInfo? = null

    override fun setup(authenticatorInfo: AuthenticatorSetupInfo?) {
        this.authenticatorInfo = authenticatorInfo
    }

    override fun loadOwnerData(): Single<Pair<AuthenticatorSetupInfo?, List<Solidity.Address>>> =
        accountsRepository.createOwnersFromPhrase(getRecoveryPhrase(), listOf(0, 1))
            .map { accounts ->
                authenticatorInfo to
                        listOfNotNull(
                            authenticatorInfo?.authenticator?.address,
                            accounts[0].address,
                            accounts[1].address
                        )
            }
            .subscribeOn(Schedulers.io())
}
