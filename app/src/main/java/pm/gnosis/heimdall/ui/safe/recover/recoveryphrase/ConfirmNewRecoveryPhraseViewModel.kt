package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import javax.inject.Inject

class ConfirmNewRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39,
    private val safeRepository: GnosisSafeRepository,
    private val recoverSafeOwnersHelper: RecoverSafeOwnersHelper
) : ConfirmNewRecoveryPhraseContract() {
    private lateinit var safeAddress: Solidity.Address
    private var browserExtensionAddress: Solidity.Address? = null

    override fun setup(safeAddress: Solidity.Address, browserExtensionAddress: Solidity.Address?) {
        this.safeAddress = safeAddress
        this.browserExtensionAddress = browserExtensionAddress
    }

    override fun loadTransaction(): Single<Pair<Solidity.Address, SafeTransaction>> =
        Single.zip(
            safeRepository.loadInfo(safeAddress).firstOrError(),
            accountsRepository.loadActiveAccount().map { it.address },
            getAddressesFromRecoveryPhrase(),
            Function3 { safeInfo: SafeInfo, appAccount: Solidity.Address, recoveryPhraseAddresses: Set<Solidity.Address> ->
                recoverSafeOwnersHelper.buildRecoverTransaction(
                    safeInfo = safeInfo,
                    addressesToKeep = listOfNotNull(appAccount, browserExtensionAddress).toSet(),
                    addressesToSwapIn = recoveryPhraseAddresses
                )
            }
        )
            .map { transaction -> safeAddress to transaction }
            .subscribeOn(Schedulers.io())

    override fun getSafeAddress(): Solidity.Address = safeAddress

    private fun getAddressesFromRecoveryPhrase() =
        Single.fromCallable { bip39.mnemonicToSeed(bip39.validateMnemonic(getRecoveryPhrase())) }
            .flatMap { seed ->
                Single.zip(
                    accountsRepository.accountFromMnemonicSeed(seed, accountIndex = 0).map { it.first },
                    accountsRepository.accountFromMnemonicSeed(seed, accountIndex = 1).map { it.first },
                    BiFunction { recoveryAccount1: Solidity.Address, recoveryAccount2: Solidity.Address ->
                        setOf(
                            recoveryAccount1,
                            recoveryAccount2
                        )
                    }
                )
            }
}
