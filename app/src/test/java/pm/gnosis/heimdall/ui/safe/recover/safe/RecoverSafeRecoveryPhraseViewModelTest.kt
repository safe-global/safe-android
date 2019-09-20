package pm.gnosis.heimdall.ui.safe.recover.safe

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
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
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract.Input
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract.ViewUpdate
import pm.gnosis.heimdall.utils.AuthenticatorInfo
import pm.gnosis.heimdall.utils.AuthenticatorSetupInfo
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asTransactionHash
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class RecoverSafeRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var helperMock: RecoverSafeOwnersHelper

    @Mock
    private lateinit var safeRepoMock: GnosisSafeRepository

    private val encryptedByteArrayConverter = EncryptedByteArray.Converter()

    private lateinit var viewModel: RecoverSafeRecoveryPhraseViewModel

    @Before
    fun setUp() {
        viewModel = RecoverSafeRecoveryPhraseViewModel(accountsRepositoryMock, helperMock, safeRepoMock)
    }

    private fun process(authenticatorInfo: AuthenticatorSetupInfo?) {
        val safeOwner: AccountsRepository.SafeOwner
        if (authenticatorInfo == null) {
            val pk = encryptedByteArrayConverter.fromStorage("owner_pk")
            safeOwner = AccountsRepository.SafeOwner(TEST_OWNER, pk)
            given(accountsRepositoryMock.createOwner()).willReturn(Single.just(safeOwner))
        } else {
            safeOwner = authenticatorInfo.safeOwner
        }
        val helperSubject = PublishSubject.create<ViewUpdate>()

        given(
            helperMock.process(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()))
            .willReturn(helperSubject)
        given(
            safeRepoMock.saveOwner(
                MockUtils.any(),
                MockUtils.any())
        ).willReturn(Completable.complete())


        val observer = TestObserver<ViewUpdate>()
        val input = Input(Observable.empty(), Observable.empty(), Observable.empty())
        viewModel.process(
            input,
            TEST_SAFE,
            authenticatorInfo
        ).subscribe(observer)

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
            TEST_TX_HASH.asTransactionHash(), SafeTransaction(
                Transaction(
                    TEST_SAFE
                ), TransactionExecutionRepository.Operation.CALL
            ),
            TEST_SAFE, 2, emptyList(), SemVer(1, 0, 0),
            TEST_GAS_TOKEN, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO
        )
        val signatures = listOf(Signature(BigInteger.TEN, BigInteger.TEN, 27))

        val error = IllegalStateException()
        given(safeRepoMock.addRecoveringSafe(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(
            Completable.error(error)
        )
        helperSubject.onNext(ViewUpdate.RecoverData(executionInfo, signatures, safeOwner))
        observer.assertValueAt(tests, ViewUpdate.RecoverDataError(error))
        then(safeRepoMock).should().addRecoveringSafe(TEST_SAFE, null, null, executionInfo, signatures)
        then(safeRepoMock).should().saveOwner(TEST_SAFE, safeOwner)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        tests++

        given(safeRepoMock.addRecoveringSafe(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(
            Completable.complete()
        )
        helperSubject.onNext(ViewUpdate.RecoverData(executionInfo, signatures, safeOwner))
        observer.assertValueAt(tests, ViewUpdate.RecoverData(executionInfo, signatures, safeOwner))
        then(safeRepoMock).should(times(2)).addRecoveringSafe(TEST_SAFE, null, null, executionInfo, signatures)
        then(safeRepoMock).should(times(2)).saveOwner(TEST_SAFE, safeOwner)
        authenticatorInfo?.let { then(safeRepoMock).should().saveAuthenticatorInfo(it.authenticator) }
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
        then(helperMock).should().process(
            input,
            TEST_SAFE,
            authenticatorInfo?.authenticator?.address,
            safeOwner
        )
        then(helperMock).shouldHaveNoMoreInteractions()

    }
    @Test
    fun processWithoutAuthenticator() {
        process(null)
    }

    @Test
    fun processWithAuthenticator() {
        val pk = encryptedByteArrayConverter.fromStorage("another_pk")
        val signingOwner = AccountsRepository.SafeOwner("0xbadad".asEthereumAddress()!!, pk)
        process(AuthenticatorSetupInfo(signingOwner, AuthenticatorInfo(AuthenticatorInfo.Type.EXTENSION, TEST_EXTENSION)))
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
        private val TEST_GAS_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_OWNER = "0xdeadbeef".asEthereumAddress()!!
        private val TEST_EXTENSION = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_TX_HASH = "0xdae721569a948b87c269ebacaa5a4a67728095e32f9e7e4626f109f27a73b40f".hexAsBigInteger()
    }
}
