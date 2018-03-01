package pm.gnosis.heimdall.ui.onboarding.account.restore

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.onboarding.SetupSafeIntroActivity
import pm.gnosis.mnemonic.*
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class RestoreAccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountsRepository: AccountsRepository,
    private val bip39: Bip39
) : RestoreAccountContract() {

    private val errorHandler = SimpleLocalizedException.Handler.Builder(context)
        .add({ it is EmptyMnemonic }, R.string.invalid_mnemonic_no_words)
        .add({ it is InvalidEntropy || it is InvalidChecksum || it is UnknownMnemonicError }, R.string.invalid_mnemonic)
        .add({ it is MnemonicNotInWordlist }, R.string.invalid_mnemonic_supported_languages)
        .build()

    override fun saveAccountWithMnemonic(mnemonic: String): Observable<Result<Intent>> =
        Single.fromCallable { bip39.validateMnemonic(mnemonic) }
            .subscribeOn(Schedulers.computation())
            .flatMap {
                accountsRepository.saveAccountFromMnemonicSeed(bip39.mnemonicToSeed(mnemonic))
                    .andThen(accountsRepository.saveMnemonic(mnemonic))
                    .toSingle({ SetupSafeIntroActivity.createIntent(context) })
                    .subscribeOn(Schedulers.io())
            }
            .onErrorResumeNext({ errorHandler.single<Intent>(it) })
            .toObservable()
            .mapToResult()
}
