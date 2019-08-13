package pm.gnosis.heimdall.ui.recoveryphrase

import android.content.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.utils.words
import javax.inject.Inject

class SetupRecoveryPhraseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bip39: Bip39
) : SetupRecoveryPhraseContract() {
    private var mnemonic: String? = null

    override fun generateRecoveryPhrase(): Single<List<String>> =
        Single.fromCallable { bip39.generateMnemonic(languageId = R.id.english) }
            .doOnSuccess { this.mnemonic = it }
            .map { it.words() }
            .subscribeOn(Schedulers.io())

    override fun getRecoveryPhrase() = mnemonic
}
