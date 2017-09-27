package pm.gnosis.heimdall.ui.onboarding

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import javax.inject.Inject

class RestoreAccountViewModel @Inject constructor(private val accountsRepository: AccountsRepository) : RestoreAccountContract() {
    override fun isValidMnemonic(mnemonic: String): Single<Boolean> {
        val words = mnemonic.split(Regex("\\s+"))
        if (words.size < 12) return Single.just(false)
        return accountsRepository.validateMnemonic(mnemonic)
                .onErrorResumeNext { Single.just(false) }
                .subscribeOn(Schedulers.computation())
    }

    override fun saveAccountWithMnemonic(mnemonic: String): Completable =
            accountsRepository.saveAccountFromMnemonic(mnemonic).andThen(accountsRepository.saveMnemonic(mnemonic))
                    .subscribeOn(Schedulers.io())
}
