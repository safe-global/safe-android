package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.models.SemVer
import pm.gnosis.heimdall.ui.safe.helpers.RecoverSafeOwnersHelper
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress

@RunWith(MockitoJUnitRunner::class)
class ConfirmNewRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var viewModel: ConfirmNewRecoveryPhraseViewModel

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var bip39Mock: Bip39

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var recoverSafeOwnersHelperMock: RecoverSafeOwnersHelper

    @Before
    fun setUp() {
        viewModel = ConfirmNewRecoveryPhraseViewModel(accountsRepositoryMock, bip39Mock, safeRepositoryMock, recoverSafeOwnersHelperMock)
        viewModel.setup(SAFE_ADDRESS, BROWSER_EXTENSION_ADDRESS)
        viewModel.setup(RECOVERY_PHRASE)
    }

    @Test
    fun loadTransaction() {
        val testObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(PHONE_ADDRESS)))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(byteArrayOf(0))
        given(bip39Mock.validateMnemonic(anyString())).willReturn(RECOVERY_PHRASE)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(RECOVERY_ADDRESS_0 to byteArrayOf(0)))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(RECOVERY_ADDRESS_1 to byteArrayOf(0)))
        given(recoverSafeOwnersHelperMock.buildRecoverTransaction(MockUtils.any(), MockUtils.any(), MockUtils.any())).willReturn(SAFE_TRANSACTION)

        viewModel.loadTransaction().subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().validateMnemonic(RECOVERY_PHRASE)
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(byteArrayOf(0), 0)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(byteArrayOf(0), 1)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(recoverSafeOwnersHelperMock).should().buildRecoverTransaction(
            safeInfo = SAFE_INFO,
            addressesToKeep = setOf(PHONE_ADDRESS, BROWSER_EXTENSION_ADDRESS),
            addressesToSwapIn = setOf(RECOVERY_ADDRESS_0, RECOVERY_ADDRESS_1)
        )
        then(recoverSafeOwnersHelperMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(SAFE_ADDRESS to SAFE_TRANSACTION)
    }

    @Test
    fun loadTransactionAccountFromMnemonicSeedError() {
        val testObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        val exception = IllegalStateException()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(PHONE_ADDRESS)))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(byteArrayOf(0))
        given(bip39Mock.validateMnemonic(anyString())).willReturn(RECOVERY_PHRASE)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(RECOVERY_ADDRESS_0 to byteArrayOf(0)))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.error(exception))

        viewModel.loadTransaction().subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().validateMnemonic(RECOVERY_PHRASE)
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(byteArrayOf(0), 0)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(byteArrayOf(0), 1)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(recoverSafeOwnersHelperMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun loadTransactionValidateMnemonicError() {
        val testObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        val exception = IllegalStateException()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(PHONE_ADDRESS)))
        given(bip39Mock.validateMnemonic(anyString())).willThrow(exception)

        viewModel.loadTransaction().subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().validateMnemonic(RECOVERY_PHRASE)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(recoverSafeOwnersHelperMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun loadTransactionMnemonicToSeedError() {
        val testObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        val exception = IllegalStateException()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(PHONE_ADDRESS)))
        given(bip39Mock.validateMnemonic(anyString())).willReturn(RECOVERY_PHRASE)
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willThrow(exception)

        viewModel.loadTransaction().subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().validateMnemonic(RECOVERY_PHRASE)
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(recoverSafeOwnersHelperMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun loadTransactionBuildRecoverTransactionError() {
        val testObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        val exception = IllegalStateException()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(Account(PHONE_ADDRESS)))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(byteArrayOf(0))
        given(bip39Mock.validateMnemonic(anyString())).willReturn(RECOVERY_PHRASE)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L)))
            .willReturn(Single.just(RECOVERY_ADDRESS_0 to byteArrayOf(0)))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L)))
            .willReturn(Single.just(RECOVERY_ADDRESS_1 to byteArrayOf(0)))
        given(recoverSafeOwnersHelperMock.buildRecoverTransaction(MockUtils.any(), MockUtils.any(), MockUtils.any())).willThrow(exception)

        viewModel.loadTransaction().subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().validateMnemonic(RECOVERY_PHRASE)
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(byteArrayOf(0), 0)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(byteArrayOf(0), 1)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(recoverSafeOwnersHelperMock).should().buildRecoverTransaction(
            safeInfo = SAFE_INFO,
            addressesToKeep = setOf(PHONE_ADDRESS, BROWSER_EXTENSION_ADDRESS),
            addressesToSwapIn = setOf(RECOVERY_ADDRESS_0, RECOVERY_ADDRESS_1)
        )
        then(recoverSafeOwnersHelperMock).shouldHaveNoMoreInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun loadTransactionLoadActiveAccountError() {
        val testObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        val exception = IllegalStateException()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.just(SAFE_INFO))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error(exception))

        viewModel.loadTransaction().subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveZeroInteractions()
        then(recoverSafeOwnersHelperMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun loadTransactionLoadInfoError() {
        val testObserver = TestObserver<Pair<Solidity.Address, SafeTransaction>>()
        val exception = IllegalStateException()
        given(safeRepositoryMock.loadInfo(MockUtils.any())).willReturn(Observable.error(exception))
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error(exception))

        viewModel.loadTransaction().subscribe(testObserver)

        then(safeRepositoryMock).should().loadInfo(SAFE_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveZeroInteractions()
        then(recoverSafeOwnersHelperMock).shouldHaveZeroInteractions()

        testObserver.assertFailure(IllegalStateException::class.java)
    }

    @Test
    fun getSafeAddress() {
        assertEquals(SAFE_ADDRESS, viewModel.getSafeAddress())
    }

    companion object {
        private const val RECOVERY_PHRASE = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        private val SAFE_ADDRESS = "0xf".asEthereumAddress()!!
        private val PHONE_ADDRESS = "0x41".asEthereumAddress()!!
        private val BROWSER_EXTENSION_ADDRESS = "0x42".asEthereumAddress()!!
        private val RECOVERY_ADDRESS_0 = "0x43".asEthereumAddress()!!
        private val RECOVERY_ADDRESS_1 = "0x44".asEthereumAddress()!!
        private val SAFE_TRANSACTION = SafeTransaction(
            Transaction(
                address = Solidity.Address(100.toBigInteger())
            ), operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
        )

        private val SAFE_INFO = SafeInfo(
            address = SAFE_ADDRESS,
            balance = Wei.ZERO,
            requiredConfirmations = 2L,
            owners = listOf(PHONE_ADDRESS, BROWSER_EXTENSION_ADDRESS, RECOVERY_ADDRESS_0, RECOVERY_ADDRESS_1),
            isOwner = true,
            modules = emptyList(),
            version = SemVer(1, 0, 0)
        )
    }
}
