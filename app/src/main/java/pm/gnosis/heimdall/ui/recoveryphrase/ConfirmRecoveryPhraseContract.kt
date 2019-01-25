package pm.gnosis.heimdall.ui.recoveryphrase

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result

abstract class ConfirmRecoveryPhraseContract : ViewModel() {
    abstract fun setup(recoveryPhrase: String)
    abstract fun getRecoveryPhrase(): String
    abstract fun getIncorrectPositions(words: List<String>): Single<Result<Set<Int>>>
    abstract fun loadRandomPositions(): Single<List<Int>>

    companion object {
        const val SELECTABLE_WORDS = 4
    }
}
