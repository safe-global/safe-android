package pm.gnosis.heimdall.ui.safe.recover.phrase

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract.Input
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract.ViewUpdate
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asTransactionHash
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class RecoverInputRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var helperMock: RecoverSafeOwnersHelper

    @Mock
    private lateinit var safeRepoMock: GnosisSafeRepository

    private lateinit var viewModel: RecoverInputRecoveryPhraseViewModel

    @Before
    fun setUp() {
        viewModel = RecoverInputRecoveryPhraseViewModel(helperMock, safeRepoMock)
    }

    @Test
    fun process() {
        val helperSubject = PublishSubject.create<InputRecoveryPhraseContract.ViewUpdate>()
        given(helperMock.process(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(helperSubject)

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        val input = Input(Observable.empty(), Observable.empty(), Observable.empty())
        viewModel.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)

        var tests = 0
        ViewUpdate::class.nestedClasses
            .filter { it != ViewUpdate.RecoverData::class && it != ViewUpdate.NoRecoveryNecessary::class }
            .forEach {
                val viewUpdate = TEST_DATA[it] ?: throw IllegalStateException("Test data missing for $it")
                helperSubject.onNext(viewUpdate)
                observer.assertValueAt(tests, viewUpdate)
                tests++
            }

        observer.assertValueCount(tests).assertNoErrors().assertNotComplete()
        then(safeRepoMock).shouldHaveZeroInteractions()

        val executionInfo = TransactionExecutionRepository.ExecuteInformation(
            TEST_TX_HASH.asTransactionHash(), SafeTransaction(Transaction(TEST_SAFE), TransactionExecutionRepository.Operation.CALL),
            TEST_EXTENSION, 2, emptyList(), BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, Wei.ZERO
        )
        val signatures = listOf(Signature(BigInteger.TEN, BigInteger.TEN, 27))

        val error = IllegalStateException()
        given(safeRepoMock.addRecoveringSafe(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(
            Completable.error(error)
        )
        helperSubject.onNext(ViewUpdate.RecoverData(executionInfo, signatures))
        observer.assertValueAt(tests, ViewUpdate.RecoverDataError(error))
        then(safeRepoMock).should().addRecoveringSafe(TEST_SAFE, null, null, executionInfo, signatures)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        tests++

        given(safeRepoMock.addRecoveringSafe(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(
            Completable.complete()
        )
        helperSubject.onNext(ViewUpdate.RecoverData(executionInfo, signatures))
        observer.assertValueAt(tests, ViewUpdate.RecoverData(executionInfo, signatures))
        then(safeRepoMock).should(times(2)).addRecoveringSafe(TEST_SAFE, null, null, executionInfo, signatures)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        tests++

        given(safeRepoMock.addSafe(MockUtils.any(), MockUtils.any())).willReturn(Completable.error(error))
        helperSubject.onNext(ViewUpdate.NoRecoveryNecessary(TEST_SAFE))
        observer.assertValueAt(tests, ViewUpdate.RecoverDataError(error))
        then(safeRepoMock).should().addSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        tests++

        given(safeRepoMock.addSafe(MockUtils.any(), MockUtils.any())).willReturn(Completable.complete())
        helperSubject.onNext(ViewUpdate.NoRecoveryNecessary(TEST_SAFE))
        observer.assertValueAt(tests, ViewUpdate.NoRecoveryNecessary(TEST_SAFE))
        then(safeRepoMock).should(times(2)).addSafe(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        tests++

        observer.assertValueCount(tests).assertNoErrors().assertNotComplete()
        then(helperMock).should().process(input, TEST_SAFE, TEST_EXTENSION)
        then(helperMock).shouldHaveNoMoreInteractions()

    }

    companion object {
        private val TEST_DATA = mapOf(
            ViewUpdate.SafeInfoError::class to ViewUpdate.SafeInfoError(UnknownHostException()),
            ViewUpdate.InputMnemonic::class to ViewUpdate.InputMnemonic,
            ViewUpdate.InvalidMnemonic::class to ViewUpdate.InvalidMnemonic,
            ViewUpdate.WrongMnemonic::class to ViewUpdate.WrongMnemonic,
            ViewUpdate.ValidMnemonic::class to ViewUpdate.ValidMnemonic,
            ViewUpdate.RecoverDataError::class to ViewUpdate.RecoverDataError(IllegalStateException())
        )
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_EXTENSION = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_TX_HASH = "0xdae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f".hexAsBigInteger()
    }
}
