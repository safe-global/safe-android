package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import javax.inject.Inject

class SafeRecoveryPhraseViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39
) : SafeRecoveryPhraseContract() {
    override fun generateMnemonic(): Single<String> = Single
        .fromCallable { bip39.generateMnemonic(languageId = R.id.english) }
        .subscribeOn(Schedulers.io())

    override fun getRecoveryAddress(safeRecoveryPhrase: String): Single<Solidity.Address> =
        accountsRepository.accountFromMnemonicSeed(bip39.mnemonicToSeed(safeRecoveryPhrase))
            .map { it.first }
}
