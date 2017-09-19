package pm.gnosis.android.app.authenticator.ui.onboarding

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.android.app.authenticator.di.ForView
import pm.gnosis.mnemonic.Bip39
import javax.inject.Inject

@ForView
class GenerateMnemonicPresenter @Inject constructor() {
    fun generateMnemonic(): Observable<String> =
            Observable.fromCallable { Bip39.generateMnemonic() }
                    .subscribeOn(Schedulers.io())

    fun savePrivateKey() {

    }
}
