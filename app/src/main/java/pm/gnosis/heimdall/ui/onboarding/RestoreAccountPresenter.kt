package pm.gnosis.heimdall.ui.onboarding

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.accounts.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.mnemonic.Bip39
import javax.inject.Inject

@ForView
class RestoreAccountPresenter @Inject constructor(private val accountsRepository: AccountsRepository) {
    fun isValidMnemonic(mnemonic: String): Single<Boolean> = Single.fromCallable {
        val words = mnemonic.split(Regex("\\s+"))
        if (words.size < 12) {
            false
        } else {
            Bip39.validateMnemmonic(mnemonic)
        }
    }.onErrorResumeNext { Single.just(false) }.subscribeOn(Schedulers.computation())

    fun saveAccountWithMnemonic(mnemonic: String): Completable =
            accountsRepository.saveAccountFromMnemonic(mnemonic).andThen(accountsRepository.saveMnemonic(mnemonic))
                    .subscribeOn(Schedulers.io())
}
