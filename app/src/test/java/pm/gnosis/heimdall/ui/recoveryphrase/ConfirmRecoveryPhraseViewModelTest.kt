package pm.gnosis.heimdall.ui.recoveryphrase

import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.words

@RunWith(MockitoJUnitRunner::class)
class ConfirmRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var viewModel: ConfirmRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = ConfirmRecoveryPhraseTestViewModel()
    }

    @Test
    fun testGetMnemonic() {
        viewModel.setup(RECOVERY_PHRASE)
        assertEquals(RECOVERY_PHRASE, viewModel.getRecoveryPhrase())
    }

    @Test
    fun getIncorrectPositionsWrongRecoveryPhrase() {
        val testObserver = TestObserver.create<Result<Set<Int>>>()
        val words = "wrong0 media athlete wrong3 rocket plate minute obey head toward coach senior".words()

        viewModel.setup(RECOVERY_PHRASE)
        viewModel.getIncorrectPositions(words).subscribe(testObserver)
        testObserver.assertValue { it is DataResult && it.data.containsAll(listOf(0, 3)) }
    }

    @Test
    fun getIncorrectPositionsCorrectRecoveryPhrase() {
        val testObserver = TestObserver.create<Result<Set<Int>>>()

        viewModel.setup(RECOVERY_PHRASE)
        viewModel.getIncorrectPositions(RECOVERY_PHRASE.words()).subscribe(testObserver)
        testObserver.assertValue { it is DataResult && it.data.isEmpty() }
    }

    @Test
    fun loadRandomPositions() {
        val testObserver = TestObserver<List<Int>>()

        viewModel.setup(RECOVERY_PHRASE)
        viewModel.loadRandomPositions().subscribe(testObserver)

        println("loadRandomPositions() = ${testObserver.values().first()}")
        testObserver.assertNoErrors()
            // The amount of returned indexes should be the same as the number defined in the contract
            .assertValue { randomPositions -> randomPositions.size == ConfirmRecoveryPhraseContract.SELECTABLE_WORDS }
            // The indexes of each word should be valid (within the bounds of the recovery phrase)
            .assertValue { randomPositions -> randomPositions.all { it >= 0 && it < RECOVERY_PHRASE.words().size } }
            // The result should always contain unique indexes
            .assertValue { randomPositions -> randomPositions.distinct().size == ConfirmRecoveryPhraseContract.SELECTABLE_WORDS }
    }

    companion object {
        const val RECOVERY_PHRASE = "degree media athlete harvest rocket plate minute obey head toward coach senior"
    }
}

class ConfirmRecoveryPhraseTestViewModel : ConfirmRecoveryPhraseViewModel()
