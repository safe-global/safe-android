package pm.gnosis.heimdall.ui.onboarding.account.create

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class GenerateMnemonicViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39
) : GenerateMnemonicContract() {
    override fun generateMnemonic(): Single<Result<String>> = Single
        .fromCallable { bip39.generateMnemonic(languageId = R.id.english) }
        .mapToResult()
        .subscribeOn(Schedulers.io())

    override fun saveAccountWithMnemonic(mnemonic: String): Single<Result<Unit>> =
        Single.fromCallable { bip39.mnemonicToSeed(mnemonic) }
            .flatMapCompletable { accountsRepository.saveAccountFromMnemonicSeed(it) }
            .andThen(accountsRepository.saveMnemonic(mnemonic))
            .andThen(Single.just(Unit))
            .mapToResult()
            .subscribeOn(Schedulers.io())
}
