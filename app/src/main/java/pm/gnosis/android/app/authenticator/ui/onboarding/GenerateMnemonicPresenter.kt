package pm.gnosis.android.app.authenticator.ui.onboarding

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okio.ByteString
import pm.gnosis.android.app.accounts.repositories.AccountsRepository
import pm.gnosis.android.app.android.utils.PreferencesManager
import pm.gnosis.android.app.authenticator.di.ForView
import pm.gnosis.android.app.authenticator.util.edit
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.mnemonic.Bip39
import javax.inject.Inject

@ForView
class GenerateMnemonicPresenter @Inject constructor(private val accountsRepository: AccountsRepository,
                                                    private val preferencesManager: PreferencesManager) {
    fun generateMnemonic(): Observable<String> =
            Observable.fromCallable { Bip39.generateMnemonic() }
                    .subscribeOn(Schedulers.io())

    fun saveAccountWithMnemonic(mnemonic: String): Completable =
            Single.fromCallable {
                val hdNode = KeyGenerator().masterNode(ByteString.of(*Bip39.mnemonicToSeed(mnemonic)))
                hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM).deriveChild(0).keyPair
            }.flatMapCompletable {
                val privateKeyBytes = it.privKeyBytes ?: throw IllegalStateException("Private key must not be null")
                accountsRepository.saveAccount(privateKeyBytes)
            }.andThen(saveMnemonicToSharedPrefs(mnemonic)).
                    subscribeOn(Schedulers.io())

    private fun saveMnemonicToSharedPrefs(mnemonic: String) = Completable.fromCallable {
        preferencesManager.prefs.edit {
            //TODO: in the future this needs to be encrypted
            putString(PreferencesManager.MNEMONIC_KEY, mnemonic)
        }
    }
}
