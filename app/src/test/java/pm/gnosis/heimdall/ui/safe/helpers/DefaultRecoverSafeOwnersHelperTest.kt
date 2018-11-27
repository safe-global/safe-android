package pm.gnosis.heimdall.ui.safe.helpers

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract.Input
import pm.gnosis.heimdall.ui.safe.mnemonic.InputRecoveryPhraseContract.ViewUpdate
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.mnemonic.EmptyMnemonic
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.tests.utils.Asserts
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHex
import java.math.BigInteger
import java.net.UnknownHostException

@RunWith(MockitoJUnitRunner::class)
class DefaultRecoverSafeOwnersHelperTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var accountsRepoMock: AccountsRepository

    @Mock
    private lateinit var bip39Mock: Bip39

    @Mock
    private lateinit var executionRepoMock: TransactionExecutionRepository

    @Mock
    private lateinit var safeRepoMock: GnosisSafeRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    private lateinit var helper: DefaultRecoverSafeOwnersHelper

    @Before
    fun setUp() {
        helper = DefaultRecoverSafeOwnersHelper(contextMock, accountsRepoMock, bip39Mock, executionRepoMock, safeRepoMock, tokenRepositoryMock)
    }

    @Test
    fun processSeedError() {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(Observable.just(SafeInfo(TEST_SAFE, Wei.ZERO, 2, TEST_OWNERS, false, emptyList())))
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some mnemonic")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willThrow(IllegalStateException())

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)

        // Test mnemonic to seed
        phraseSubject.onNext("this is probably not a valid mnemonic")
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.WrongMnemonic)
        then(bip39Mock).should().validateMnemonic("this is probably not a valid mnemonic")
        then(bip39Mock).should().mnemonicToSeed("some mnemonic")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun processInvalidMnemonic() {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(Observable.just(SafeInfo(TEST_SAFE, Wei.ZERO, 2, TEST_OWNERS, false, emptyList())))
        given(bip39Mock.validateMnemonic(MockUtils.any())).willThrow(EmptyMnemonic(""))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)
        phraseSubject.onNext("")

        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.InvalidMnemonic)
        then(bip39Mock).should().validateMnemonic("")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun processSafeInfoRetry() {
        contextMock.mockGetString()
        var tests = 0

        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any())).willReturn(Observable.error(UnknownHostException()))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)

        // Test safe info failure
        observer.assertValueCount(tests + 1)
            .assertValueAt(tests, ViewUpdate.SafeInfoError(SimpleLocalizedException(R.string.error_check_internet_connection.toString())))
        // First call should happen automatically on subscribe
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        tests++

        // Test safe info success
        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(Observable.just(SafeInfo(TEST_SAFE, Wei.ZERO, 2, TEST_OWNERS, false, emptyList())))
        retrySubject.onNext(Unit)
        observer.assertValueCount(tests + 1).assertValueAt(tests, ViewUpdate.InputMnemonic)
        then(safeRepoMock).should(times(2)).loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun processAccountFromSeedError() {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(Observable.just(SafeInfo(TEST_SAFE, Wei.ZERO, 2, TEST_OWNERS, false, emptyList())))
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.error(IllegalArgumentException()))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.WrongMnemonic)

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    private fun testProcessInvalidOwnerCount(
        extension: Solidity.Address?,
        owners: List<Solidity.Address>
    ) {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, owners, false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, extension).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.WrongMnemonic)

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun processInvalidOwnerCountWithExtension() {
        testProcessInvalidOwnerCount(TEST_EXTENSION, listOf(TEST_APP, TEST_RECOVER_1, TEST_RECOVER_2))
    }

    @Test
    fun processInvalidOwnerCountWithoutExtension() {
        testProcessInvalidOwnerCount(null, listOf(TEST_APP, TEST_EXTENSION, TEST_RECOVER_1, TEST_RECOVER_2))
    }

    @Test
    fun processInvalidRecoveryKey1NotPresent() {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, listOf(TEST_APP, TEST_EXTENSION, TEST_SAFE, TEST_RECOVER_2), false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.WrongMnemonic)

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun processInvalidRecoveryKey2NotPresent() {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, listOf(TEST_APP, TEST_EXTENSION, TEST_SAFE, TEST_RECOVER_1), false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.WrongMnemonic)

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun processLoadAccountError() {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, listOf(TEST_APP, TEST_EXTENSION, TEST_RECOVER_1, TEST_RECOVER_2), false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))
        given(accountsRepoMock.loadActiveAccount()).willReturn(Single.error(EmptyResultSetException("")))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_EXTENSION).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.WrongMnemonic)

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).should().loadActiveAccount()
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    private fun testProcessNothingToRecover(extension: Solidity.Address?) {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, listOfNotNull(TEST_APP, extension, TEST_RECOVER_1, TEST_RECOVER_2), false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))
        given(accountsRepoMock.loadActiveAccount()).willReturn(Single.just(Account(TEST_APP)))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, extension).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.NoRecoveryNecessary(TEST_SAFE))

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).should().loadActiveAccount()
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
    }

    @Test
    fun processNothingToRecoverWithExtension() {
        testProcessNothingToRecover(TEST_EXTENSION)
    }

    @Test
    fun processNothingToRecoverWithoutExtension() {
        testProcessNothingToRecover(null)
    }

    private fun testRecoverPayload(
        app: Solidity.Address, extension: Solidity.Address?, recoverer: Solidity.Address,
        operation: TransactionExecutionRepository.Operation, data: String,
        owners: List<Solidity.Address> = listOf(TEST_APP, TEST_EXTENSION, TEST_RECOVER_1, TEST_RECOVER_2)
    ) {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(tokenRepositoryMock.loadPaymentToken()).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, owners, false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))
        given(accountsRepoMock.loadActiveAccount()).willReturn(Single.just(Account(app)))
        given(executionRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willAnswer {
                val tx = it.arguments[2] as SafeTransaction
                Single.just(
                    TransactionExecutionRepository.ExecuteInformation(
                        TEST_HASH.toHex(), tx, TEST_SAFE, 2, owners,
                        TEST_GAS_TOKEN, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO
                    )
                )
            }
        given(executionRepoMock.calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.just(TEST_HASH))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, extension).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        createSubject.onNext(Unit)
        observer
            .assertValueAt(0, ViewUpdate.InputMnemonic)
            .assertValueAt(1, ViewUpdate.ValidMnemonic)
            .assertValueAt(2) {
                it is ViewUpdate.RecoverData &&
                        it.signatures.size == 2 &&
                        it.executionInfo.transactionHash == TEST_HASH.toHex() &&
                        it.executionInfo.transaction.operation == operation &&
                        it.executionInfo.transaction.wrapped.address == recoverer &&
                        it.executionInfo.transaction.wrapped.value == null &&
                        it.executionInfo.transaction.wrapped.nonce == null && // Nonce was not set in this test
                        it.executionInfo.transaction.wrapped.data == data
            }
            .assertValueCount(3)

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).should().loadActiveAccount()
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).should().loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(executionRepoMock).should()
            .calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(executionRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

    }

    @Test
    fun processReplaceApp() {
        testRecoverPayload(
            TEST_NEW_APP,
            TEST_EXTENSION,
            TEST_SAFE,
            TransactionExecutionRepository.Operation.CALL,
            "0xe318b52b000000000000000000000000000000000000000000000000000000000000000100000000000000000000000071de9579cd3857ce70058a1ce19e3d8894f65ab900000000000000000000000031b98d14007bdee637298086988a0bbd31184523"
        )
    }

    @Test
    fun processReplaceAppWithoutExtension() {
        testRecoverPayload(
            TEST_NEW_APP,
            null,
            TEST_SAFE,
            TransactionExecutionRepository.Operation.CALL,
            "0xe318b52b000000000000000000000000000000000000000000000000000000000000000100000000000000000000000071de9579cd3857ce70058a1ce19e3d8894f65ab900000000000000000000000031b98d14007bdee637298086988a0bbd31184523",
            listOf(TEST_APP, TEST_RECOVER_1, TEST_RECOVER_2)
        )
    }

    @Test
    fun processReplaceExtension() {
        testRecoverPayload(
            TEST_APP,
            TEST_NEW_EXTENSION,
            TEST_SAFE,
            TransactionExecutionRepository.Operation.CALL,
            "0xe318b52b00000000000000000000000071de9579cd3857ce70058a1ce19e3d8894f65ab9000000000000000000000000c2ac20b3bb950c087f18a458db68271325a481320000000000000000000000001e6534e09b2b0dc5ea965d0ce84ab07a4bd56b38"
        )
    }

    @Test
    fun processReplaceAppAndExtension() {
        testRecoverPayload(
            TEST_NEW_APP,
            TEST_NEW_EXTENSION,
            "0xe74d6af1670fb6560dd61ee29eb57c7bc027ce4e".asEthereumAddress()!!, // MultiSend address
            TransactionExecutionRepository.Operation.DELEGATE_CALL,
            "0x8d80ff0a" +
                    "0000000000000000000000000000000000000000000000000000000000000020" +
                    "0000000000000000000000000000000000000000000000000000000000000240" +
                    // First replace transaction
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000080" +
                    "0000000000000000000000000000000000000000000000000000000000000064" +
                    "e318b52b00000000000000000000000071de9579cd3857ce70058a1ce19e3d8894f65ab9000000000000000000000000c2ac20b3bb950c087f18a458db68271325a481320000000000000000000000001e6534e09b2b0dc5ea965d0ce84ab07a4bd56b38" +
                    "00000000000000000000000000000000000000000000000000000000" + // Padding
                    // Second replace transaction
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000080" +
                    "0000000000000000000000000000000000000000000000000000000000000064" +
                    "e318b52b000000000000000000000000000000000000000000000000000000000000000100000000000000000000000071de9579cd3857ce70058a1ce19e3d8894f65ab900000000000000000000000031b98d14007bdee637298086988a0bbd31184523" +
                    "00000000000000000000000000000000000000000000000000000000" // Padding
        )
    }

    @Test
    fun processLoadPaymentTokenError() {
        contextMock.mockGetString()
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, listOf(TEST_APP, TEST_EXTENSION, TEST_RECOVER_1, TEST_RECOVER_2), false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))
        given(accountsRepoMock.loadActiveAccount()).willReturn(Single.just(Account(TEST_NEW_APP)))
        given(tokenRepositoryMock.loadPaymentToken()).willReturn(Single.error(NotImplementedError()))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_NEW_EXTENSION).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        createSubject.onNext(Unit)
        observer.assertValues(
            ViewUpdate.InputMnemonic,
            ViewUpdate.ValidMnemonic,
            ViewUpdate.RecoverDataError(SimpleLocalizedException(R.string.error_check_internet_connection.toString()))
        )

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).should().loadActiveAccount()
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun processExecuteInfoError() {
        contextMock.mockGetString()
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, listOf(TEST_APP, TEST_EXTENSION, TEST_RECOVER_1, TEST_RECOVER_2), false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))
        given(accountsRepoMock.loadActiveAccount()).willReturn(Single.just(Account(TEST_NEW_APP)))
        given(tokenRepositoryMock.loadPaymentToken()).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(executionRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.error(UnknownHostException()))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_NEW_EXTENSION).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        createSubject.onNext(Unit)
        observer.assertValues(
            ViewUpdate.InputMnemonic,
            ViewUpdate.ValidMnemonic,
            ViewUpdate.RecoverDataError(SimpleLocalizedException(R.string.error_check_internet_connection.toString()))
        )

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).should().loadActiveAccount()
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).should().loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(executionRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun processHashError() {
        val phraseSubject = PublishSubject.create<CharSequence>()
        val retrySubject = PublishSubject.create<Unit>()
        val createSubject = PublishSubject.create<Unit>()
        val input = Input(phraseSubject, retrySubject, createSubject)

        given(safeRepoMock.loadInfo(MockUtils.any()))
            .willReturn(
                Observable.just(
                    SafeInfo(TEST_SAFE, Wei.ZERO, 2, listOf(TEST_APP, TEST_EXTENSION, TEST_RECOVER_1, TEST_RECOVER_2), false, emptyList())
                )
            )
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn("some new mnemonic!")
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(TEST_SEED)
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(TEST_RECOVER_1 to TEST_RECOVER_1_KEY))
        given(accountsRepoMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(TEST_RECOVER_2 to TEST_RECOVER_2_KEY))
        given(accountsRepoMock.loadActiveAccount()).willReturn(Single.just(Account(TEST_NEW_APP)))
        val execInfo = TransactionExecutionRepository.ExecuteInformation(
            TEST_HASH.toHex(), SafeTransaction(Transaction(TEST_SAFE), TransactionExecutionRepository.Operation.CALL), TEST_SAFE, 2, TEST_OWNERS,
            TEST_GAS_TOKEN, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO
        )
        given(tokenRepositoryMock.loadPaymentToken()).willReturn(Single.just(ERC20Token.ETHER_TOKEN))
        given(executionRepoMock.loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(Single.just(execInfo))
        val error = IllegalStateException()
        given(executionRepoMock.calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any()))
            .willReturn(Single.error(error))

        val observer = TestObserver<InputRecoveryPhraseContract.ViewUpdate>()
        helper.process(input, TEST_SAFE, TEST_NEW_EXTENSION).subscribe(observer)

        phraseSubject.onNext("this is not a valid mnemonic!")
        createSubject.onNext(Unit)
        observer.assertValues(ViewUpdate.InputMnemonic, ViewUpdate.ValidMnemonic, ViewUpdate.RecoverDataError(error))

        then(bip39Mock).should().validateMnemonic("this is not a valid mnemonic!")
        then(bip39Mock).should().mnemonicToSeed("some new mnemonic!")
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 0L)
        then(accountsRepoMock).should().accountFromMnemonicSeed(TEST_SEED, 1L)
        then(accountsRepoMock).should().loadActiveAccount()
        then(accountsRepoMock).shouldHaveNoMoreInteractions()
        then(safeRepoMock).should().loadInfo(TEST_SAFE)
        then(safeRepoMock).shouldHaveNoMoreInteractions()
        then(executionRepoMock).should().loadExecuteInformation(MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(executionRepoMock).should()
            .calculateHash(MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any(), MockUtils.any())
        then(executionRepoMock).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadPaymentToken()
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun buildRecoverTransactionSwapLastOwner() {
        val ownerA = Solidity.Address(10.toBigInteger())
        val ownerB = Solidity.Address(11.toBigInteger())
        val ownerC = Solidity.Address(12.toBigInteger())
        val ownerD = Solidity.Address(13.toBigInteger())

        val safeInfo = SafeInfo(TEST_SAFE, Wei.ZERO, 1, listOf(ownerA, ownerC, ownerB), false, emptyList())

        val expectedSafeTransaction = SafeTransaction(
            Transaction(
                TEST_SAFE,
                data = "0xe318b52b" + // Swap owner method
                        "000000000000000000000000000000000000000000000000000000000000000c" + // Previous Owner
                        "000000000000000000000000000000000000000000000000000000000000000b" + // Old Owner
                        "000000000000000000000000000000000000000000000000000000000000000d"   // New Owner
            ), operation = TransactionExecutionRepository.Operation.CALL
        )
        assertEquals(
            expectedSafeTransaction,
            helper.buildRecoverTransaction(safeInfo, addressesToKeep = setOf(ownerA), addressesToSwapIn = setOf(ownerD, ownerC))
        )
    }

    @Test
    fun buildRecoverTransactionSwapMiddleOwners() {
        val ownerA = Solidity.Address(10.toBigInteger())
        val ownerB = Solidity.Address(11.toBigInteger())
        val ownerC = Solidity.Address(12.toBigInteger())
        val ownerD = Solidity.Address(13.toBigInteger())
        val ownerE = Solidity.Address(14.toBigInteger())
        val ownerF = Solidity.Address(15.toBigInteger())

        val safeInfo = SafeInfo(TEST_SAFE, Wei.ZERO, 1, listOf(ownerA, ownerB, ownerC, ownerD), false, emptyList())


        val expectedSafeTransaction = SafeTransaction(
            Transaction(
                BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!,
                data = "0x8d80ff0a" + // Multi send method
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000240" +
                        "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                        "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                        "0000000000000000000000000000000000000000000000000000000000000000" +
                        "0000000000000000000000000000000000000000000000000000000000000080" +
                        "0000000000000000000000000000000000000000000000000000000000000064" +
                        "e318b52b" + // Swap owner method
                        "000000000000000000000000000000000000000000000000000000000000000c" + // Previous Owner
                        "000000000000000000000000000000000000000000000000000000000000000d" + // Old Owner
                        "000000000000000000000000000000000000000000000000000000000000000f" + // New Owner
                        "00000000000000000000000000000000000000000000000000000000" + // Padding
                        "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                        "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                        "0000000000000000000000000000000000000000000000000000000000000000" +
                        "0000000000000000000000000000000000000000000000000000000000000080" +
                        "0000000000000000000000000000000000000000000000000000000000000064" +
                        "e318b52b" + // Swap owner method
                        "000000000000000000000000000000000000000000000000000000000000000b" + // Previous Owner
                        "000000000000000000000000000000000000000000000000000000000000000c" + // Old Owner
                        "000000000000000000000000000000000000000000000000000000000000000e" + // New Owner
                        "00000000000000000000000000000000000000000000000000000000" // Padding
            ), operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
        )
        assertEquals(
            expectedSafeTransaction,
            helper.buildRecoverTransaction(safeInfo, addressesToKeep = setOf(ownerA, ownerB), addressesToSwapIn = setOf(ownerE, ownerF))
        )
    }

    @Test
    fun buildRecoverTransactionSwapInitialOwners() {
        val ownerA = Solidity.Address(10.toBigInteger())
        val ownerB = Solidity.Address(11.toBigInteger())
        val ownerC = Solidity.Address(12.toBigInteger())
        val ownerD = Solidity.Address(13.toBigInteger())
        val ownerE = Solidity.Address(14.toBigInteger())
        val ownerF = Solidity.Address(15.toBigInteger())

        val safeInfo = SafeInfo(TEST_SAFE, Wei.ZERO, 1, listOf(ownerA, ownerB, ownerC, ownerD), false, emptyList())

        val expectedSafeTransaction = SafeTransaction(
            Transaction(
                BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!,
                data = "0x8d80ff0a" + // Multi send method
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000240" +
                        "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                        "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                        "0000000000000000000000000000000000000000000000000000000000000000" +
                        "0000000000000000000000000000000000000000000000000000000000000080" +
                        "0000000000000000000000000000000000000000000000000000000000000064" +
                        "e318b52b" + // Swap owner method
                        "000000000000000000000000000000000000000000000000000000000000000a" + // Previous Owner
                        "000000000000000000000000000000000000000000000000000000000000000b" + // Old Owner
                        "000000000000000000000000000000000000000000000000000000000000000f" + // New Owner
                        "00000000000000000000000000000000000000000000000000000000" + // Padding
                        "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                        "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                        "0000000000000000000000000000000000000000000000000000000000000000" +
                        "0000000000000000000000000000000000000000000000000000000000000080" +
                        "0000000000000000000000000000000000000000000000000000000000000064" +
                        "e318b52b" + // Swap owner method
                        "0000000000000000000000000000000000000000000000000000000000000001" + // Previous Owner
                        "000000000000000000000000000000000000000000000000000000000000000a" + // Old Owner
                        "000000000000000000000000000000000000000000000000000000000000000e" + // New Owner
                        "00000000000000000000000000000000000000000000000000000000" // Padding
            ), operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
        )
        assertEquals(
            expectedSafeTransaction,
            helper.buildRecoverTransaction(safeInfo, addressesToKeep = setOf(ownerC, ownerD), addressesToSwapIn = setOf(ownerE, ownerF))
        )
    }

    @Test
    fun buildRecoverTransactionSwapInitialAndMiddle() {
        val ownerA = Solidity.Address(10.toBigInteger())
        val ownerB = Solidity.Address(11.toBigInteger())
        val ownerC = Solidity.Address(12.toBigInteger())
        val ownerD = Solidity.Address(13.toBigInteger())
        val ownerE = Solidity.Address(14.toBigInteger())
        val ownerF = Solidity.Address(15.toBigInteger())

        val safeInfo = SafeInfo(TEST_SAFE, Wei.ZERO, 1, listOf(ownerA, ownerB, ownerC, ownerD), false, emptyList())

        val expectedSafeTransaction = SafeTransaction(
            Transaction(
                BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!,
                data = "0x8d80ff0a" + // Multi send method
                        "0000000000000000000000000000000000000000000000000000000000000020" +
                        "0000000000000000000000000000000000000000000000000000000000000240" +
                        "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                        "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                        "0000000000000000000000000000000000000000000000000000000000000000" +
                        "0000000000000000000000000000000000000000000000000000000000000080" +
                        "0000000000000000000000000000000000000000000000000000000000000064" +
                        "e318b52b" + // Swap owner method
                        "000000000000000000000000000000000000000000000000000000000000000c" + // Previous Owner
                        "000000000000000000000000000000000000000000000000000000000000000d" + // Old Owner
                        "000000000000000000000000000000000000000000000000000000000000000f" + // New Owner
                        "00000000000000000000000000000000000000000000000000000000" + // Padding
                        "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                        "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                        "0000000000000000000000000000000000000000000000000000000000000000" +
                        "0000000000000000000000000000000000000000000000000000000000000080" +
                        "0000000000000000000000000000000000000000000000000000000000000064" +
                        "e318b52b" + // Swap owner method
                        "0000000000000000000000000000000000000000000000000000000000000001" + // Previous Owner
                        "000000000000000000000000000000000000000000000000000000000000000a" + // Old Owner
                        "000000000000000000000000000000000000000000000000000000000000000e" + // New Owner
                        "00000000000000000000000000000000000000000000000000000000" // Padding
            ), operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
        )
        assertEquals(
            expectedSafeTransaction,
            helper.buildRecoverTransaction(safeInfo, addressesToKeep = setOf(ownerB, ownerC), addressesToSwapIn = setOf(ownerE, ownerF))
        )
    }

    @Test
    fun buildRecoverTransactionInvalidSwap() {
        val ownerA = Solidity.Address(10.toBigInteger())
        val ownerB = Solidity.Address(11.toBigInteger())
        val ownerC = Solidity.Address(12.toBigInteger())
        val ownerD = Solidity.Address(13.toBigInteger())
        val ownerE = Solidity.Address(14.toBigInteger())
        val ownerF = Solidity.Address(15.toBigInteger())

        val safeInfo = SafeInfo(TEST_SAFE, Wei.ZERO, 1, listOf(ownerA, ownerB, ownerC, ownerD), false, emptyList())

        Asserts.assertThrow(
            test = {
                helper.buildRecoverTransaction(
                    safeInfo,
                    addressesToKeep = setOf(ownerA, ownerB, ownerC),
                    addressesToSwapIn = setOf(ownerE, ownerF)
                )
            }, throwablePredicate = { it is java.lang.IllegalStateException && it.message == "Couldn't add all addresses" }
        )
    }

    companion object {
        private val TEST_SEED = "Better Safe Than Sorry".toByteArray()
        private val TEST_HASH = Sha3Utils.keccak(TEST_SEED)
        private val TEST_GAS_TOKEN = ERC20Token.ETHER_TOKEN.address
        private val TEST_SAFE = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
        private val TEST_APP = "0x71De9579cD3857ce70058a1ce19e3d8894f65Ab9".asEthereumAddress()!!
        private val TEST_NEW_APP = "0x31B98D14007bDEe637298086988A0bBd31184523".asEthereumAddress()!!
        private val TEST_EXTENSION = "0xC2AC20b3Bb950C087f18a458DB68271325a48132".asEthereumAddress()!!
        private val TEST_NEW_EXTENSION = "0x1e6534e09b2B0Dc5EA965D0cE84AB07A4bd56B38".asEthereumAddress()!!
        private val TEST_RECOVER_1 = "0x979861dF79C7408553aAF20c01Cfb3f81CCf9341".asEthereumAddress()!!
        private val TEST_RECOVER_2 = "0x8e6A5aDb2B88257A3DAc7A76A7B4EcaCdA090b66".asEthereumAddress()!!
        private val TEST_RECOVER_1_KEY = Sha3Utils.keccak(TEST_RECOVER_1.value.toByteArray())
        private val TEST_RECOVER_2_KEY = Sha3Utils.keccak(TEST_RECOVER_2.value.toByteArray())
        private val TEST_OWNERS = listOf(TEST_APP, TEST_EXTENSION, TEST_RECOVER_1, TEST_RECOVER_2)
    }
}
