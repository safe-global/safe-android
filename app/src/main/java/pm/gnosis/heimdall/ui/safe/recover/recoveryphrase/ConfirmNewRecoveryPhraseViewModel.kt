package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import io.reactivex.Single
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.model.Solidity
import javax.inject.Inject

class ConfirmNewRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val safeRepository: GnosisSafeRepository,
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : ConfirmNewRecoveryPhraseContract() {
    private lateinit var safeAddress: Solidity.Address
    private var authenticatorAddress: Solidity.Address? = null

    override fun setup(safeAddress: Solidity.Address, authenticatorAddress: Solidity.Address?) {
        this.safeAddress = safeAddress
        this.authenticatorAddress = authenticatorAddress
    }

    override fun loadTransaction(): Single<Pair<Solidity.Address, SafeTransaction>> =
        Single.zip(
            safeRepository.loadInfo(safeAddress).firstOrError(),
            accountsRepository.signingOwner(safeAddress),
            getAddressesFromRecoveryPhrase(),
            Function3 { safeInfo: SafeInfo, appAccount: AccountsRepository.SafeOwner, recoveryPhraseAddresses: Set<Solidity.Address> ->
                recoverSafeOwnersHelper.buildRecoverTransaction(
                    safeInfo = safeInfo,
                    addressesToKeep = listOfNotNull(appAccount.address, authenticatorAddress).toSet(),
                    addressesToSwapIn = recoveryPhraseAddresses
                )
            }
        )
            .map { transaction -> safeAddress to transaction }
            .subscribeOn(Schedulers.io())

    override fun getSafeAddress(): Solidity.Address = safeAddress

    private fun getAddressesFromRecoveryPhrase() =
        accountsRepository.createOwnersFromPhrase(getRecoveryPhrase(), listOf(0, 1))
            .map { accounts -> setOf(accounts[0].address, accounts[1].address) }
}
