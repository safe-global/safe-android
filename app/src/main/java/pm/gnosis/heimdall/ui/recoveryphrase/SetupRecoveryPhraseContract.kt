package pm.gnosis.heimdall.ui.recoveryphrase

import androidx.lifecycle.ViewModel
import io.reactivex.Single

abstract class SetupRecoveryPhraseContract : ViewModel() {
    abstract fun generateRecoveryPhrase(): Single<List<String>>
    abstract fun getRecoveryPhrase(): String?
}
