package pm.gnosis.heimdall.ui.recoveryphrase

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.words
import java.security.SecureRandom

abstract class ConfirmRecoveryPhraseViewModel : ConfirmRecoveryPhraseContract() {
    private lateinit var recoveryPhrase: String

    override fun setup(recoveryPhrase: String) {
        this.recoveryPhrase = recoveryPhrase
    }

    override fun getRecoveryPhrase() = recoveryPhrase

    override fun getIncorrectPositions(words: List<String>): Single<Result<Set<Int>>> =
        Single.fromCallable {
            val recoveryWords = recoveryPhrase.words()

            require(words.size == recoveryWords.size)

            words.asSequence().mapIndexedNotNull { index, word ->
                if (word != recoveryWords[index]) index
                else null
            }.toSet()
        }
            .subscribeOn(Schedulers.computation())
            .mapToResult()

    override fun loadRandomPositions(): Single<List<Int>> =
        Single
            .fromCallable { (0 until recoveryPhrase.words().size).shuffled(SecureRandom()).take(SELECTABLE_WORDS) }
            .subscribeOn(Schedulers.computation())
}
