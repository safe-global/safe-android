package pm.gnosis.heimdall.ui.onboarding

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.mnemonic.Bip39ValidationResult
import pm.gnosis.mnemonic.UnknownMnemonicError
import javax.inject.Inject

class RestoreAccountViewModel @Inject constructor(private val accountsRepository: AccountsRepository) : RestoreAccountContract() {
    override fun isValidMnemonic(mnemonic: String): Single<Bip39ValidationResult> =
            accountsRepository.validateMnemonic(mnemonic)
                    .onErrorReturn { UnknownMnemonicError(mnemonic) }
                    .subscribeOn(Schedulers.computation())


    override fun saveAccountWithMnemonic(mnemonic: String): Completable =
            accountsRepository.saveAccountFromMnemonic(mnemonic).andThen(accountsRepository.saveMnemonic(mnemonic))
                    .subscribeOn(Schedulers.io())
}
