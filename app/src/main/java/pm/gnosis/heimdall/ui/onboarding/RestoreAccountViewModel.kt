package pm.gnosis.heimdall.ui.onboarding

import android.content.Context
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.heimdall.ui.main.MainActivity
import pm.gnosis.mnemonic.*
import pm.gnosis.mnemonic.wordlists.BIP39_WORDLISTS
import javax.inject.Inject

class RestoreAccountViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val accountsRepository: AccountsRepository
) : RestoreAccountContract() {

    private val errorHandler = LocalizedException.Handler.Builder(context)
            .add({ it is InvalidEntropy || it is InvalidChecksum || it is UnknownMnemonicError }, R.string.invalid_mnemonic)
            .add({ it is MnemonicNotInWordlist }, { _, _ ->
                val wordLists = BIP39_WORDLISTS.keys.joinToString()
                context.getString(R.string.invalid_mnemonic_supported_languages, wordLists)
            })
            .add({ it is EmptyMnemonic }, R.string.invalid_mnemonic_no_words)
            .build()

    override fun saveAccountWithMnemonic(mnemonic: String): Observable<Result<Intent>> =
            accountsRepository.validateMnemonic(mnemonic)
                    .subscribeOn(Schedulers.computation())
                    .flatMap {
                        accountsRepository.saveAccountFromMnemonic(it)
                                .andThen(accountsRepository.saveMnemonic(mnemonic))
                                .toSingle({ MainActivity.createIntent(context) })
                                .subscribeOn(Schedulers.io())
                    }
                    .onErrorResumeNext({ errorHandler.single<Intent>(it) })
                    .toObservable()
                    .mapToResult()
}
