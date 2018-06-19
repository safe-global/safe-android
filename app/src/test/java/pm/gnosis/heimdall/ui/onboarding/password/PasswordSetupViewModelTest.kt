package pm.gnosis.heimdall.ui.onboarding.password

import android.content.Context
import android.content.Intent
import io.reactivex.Completable
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
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import pm.gnosis.tests.utils.mockGetString

@RunWith(MockitoJUnitRunner::class)
class PasswordSetupViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    @Mock
    private lateinit var accountsRepository: AccountsRepository

    @Mock
    private lateinit var pushServiceRepository: PushServiceRepository

    @Mock
    private lateinit var bip39: Bip39

    private lateinit var viewModel: PasswordSetupViewModel

    @Before
    fun setUp() {
        viewModel = PasswordSetupViewModel(contextMock, accountsRepository, encryptionManagerMock, pushServiceRepository, bip39)
    }

    @Test
    fun emptyPassword() {
        val testObserver = TestObserver.create<Result<List<Pair<PasswordValidationCondition, Boolean>>>>()

        viewModel.validatePassword("").subscribe(testObserver)

        testObserver.assertResult(
            DataResult(
                listOf(
                    PasswordValidationCondition.NON_IDENTICAL_CHARACTERS to false,
                    PasswordValidationCondition.MINIMUM_CHARACTERS to false,
                    PasswordValidationCondition.ONE_NUMBER_ONE_LETTER to false
                )
            )
        )
    }

    @Test
    fun identicalCharacters() {
        val testObserver = TestObserver.create<Result<List<Pair<PasswordValidationCondition, Boolean>>>>()

        viewModel.validatePassword("aaabbb111").subscribe(testObserver)

        testObserver.assertResult(
            DataResult(
                listOf(
                    PasswordValidationCondition.NON_IDENTICAL_CHARACTERS to false,
                    PasswordValidationCondition.MINIMUM_CHARACTERS to true,
                    PasswordValidationCondition.ONE_NUMBER_ONE_LETTER to true
                )
            )
        )
    }

    @Test
    fun minimumCharacters() {
        val testObserver = TestObserver.create<Result<List<Pair<PasswordValidationCondition, Boolean>>>>()

        viewModel.validatePassword("acbb1").subscribe(testObserver)

        testObserver.assertResult(
            DataResult(
                listOf(
                    PasswordValidationCondition.NON_IDENTICAL_CHARACTERS to true,
                    PasswordValidationCondition.MINIMUM_CHARACTERS to false,
                    PasswordValidationCondition.ONE_NUMBER_ONE_LETTER to true
                )
            )
        )
    }

    @Test
    fun oneNumberOneLetter() {
        val testObserver = TestObserver.create<Result<List<Pair<PasswordValidationCondition, Boolean>>>>()

        viewModel.validatePassword("a1").subscribe(testObserver)

        testObserver.assertResult(
            DataResult(
                listOf(
                    PasswordValidationCondition.NON_IDENTICAL_CHARACTERS to true,
                    PasswordValidationCondition.MINIMUM_CHARACTERS to false,
                    PasswordValidationCondition.ONE_NUMBER_ONE_LETTER to true
                )
            )
        )
    }

    @Test
    fun validPassword() {
        val testObserver = TestObserver.create<Result<List<Pair<PasswordValidationCondition, Boolean>>>>()

        viewModel.validatePassword("asdqwe123").subscribe(testObserver)

        testObserver.assertResult(
            DataResult(
                listOf(
                    PasswordValidationCondition.NON_IDENTICAL_CHARACTERS to true,
                    PasswordValidationCondition.MINIMUM_CHARACTERS to true,
                    PasswordValidationCondition.ONE_NUMBER_ONE_LETTER to true
                )
            )
        )
    }

    @Test
    fun setupPasswordNotSame() {
        val observer = createObserver()

        viewModel.createAccount(Sha3Utils.keccak("111111".toByteArray()), "123456").subscribe(observer)

        then(encryptionManagerMock).shouldHaveZeroInteractions()
        then(pushServiceRepository).shouldHaveZeroInteractions()
        observer.assertValue { it is ErrorResult && it.error is PasswordInvalidException && (it.error as PasswordInvalidException).reason is PasswordsNotEqual }
    }

    @Test
    fun setupPasswordException() {
        contextMock.mockGetString()
        val observer = createObserver()
        val exception = Exception()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))

        viewModel.createAccount(Sha3Utils.keccak("111111".toByteArray()), "111111").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepository).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.password_error_saving)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.password_error_saving.toString())))
    }

    @Test
    fun createAccountErrorPassword() {
        contextMock.mockGetString()
        val observer = createObserver()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(false))

        viewModel.createAccount(Sha3Utils.keccak("111111".toByteArray()), "111111").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepository).shouldHaveZeroInteractions()
        then(contextMock).should().getString(R.string.password_error_saving)
        observer.assertResult(ErrorResult(SimpleLocalizedException(R.string.password_error_saving.toString())))
    }

    @Test
    fun createAccountSuccess() {
        val observer = createObserver()
        val testCompletable = TestCompletable()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(true))
        given(bip39.generateMnemonic(anyInt(), anyInt())).willReturn("mnemonic")
        given(bip39.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(ByteArray(0))
        given(accountsRepository.saveAccountFromMnemonicSeed(MockUtils.any(), anyLong())).willReturn(testCompletable)
        given(encryptionManagerMock.canSetupFingerprint()).willReturn(true)

        viewModel.createAccount(Sha3Utils.keccak("111111".toByteArray()), "111111").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("111111".toByteArray())
        then(encryptionManagerMock).should().canSetupFingerprint()
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepository).should().syncAuthentication(true)
        then(pushServiceRepository).shouldHaveNoMoreInteractions()
        then(bip39).should().generateMnemonic(languageId = R.id.english)
        then(bip39).should().mnemonicToSeed("mnemonic")
        then(bip39).shouldHaveNoMoreInteractions()
        then(accountsRepository).should().saveAccountFromMnemonicSeed(ByteArray(0))
        then(accountsRepository).shouldHaveNoMoreInteractions()
        observer.assertValue { it is DataResult }.assertComplete()
        assertEquals(1, testCompletable.callCount)
    }

    @Test
    fun createAccountError() {
        val observer = createObserver()
        val exception = Exception()
        given(encryptionManagerMock.setupPassword(MockUtils.any(), MockUtils.any())).willReturn(Single.just(true))
        given(bip39.generateMnemonic(anyInt(), anyInt())).willReturn("mnemonic")
        given(bip39.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(ByteArray(0))
        given(accountsRepository.saveAccountFromMnemonicSeed(MockUtils.any(), anyLong())).willReturn(Completable.error(exception))

        viewModel.createAccount(Sha3Utils.keccak("111111".toByteArray()), "111111").subscribe(observer)

        then(encryptionManagerMock).should().setupPassword("111111".toByteArray())
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        then(pushServiceRepository).shouldHaveZeroInteractions()
        then(bip39).should().generateMnemonic(languageId = R.id.english)
        then(bip39).should().mnemonicToSeed("mnemonic")
        then(bip39).shouldHaveNoMoreInteractions()
        then(accountsRepository).should().saveAccountFromMnemonicSeed(ByteArray(0))
        then(accountsRepository).shouldHaveNoMoreInteractions()
        observer.assertResult(ErrorResult(exception))
    }

    @Test
    fun passwordToHash() {
        val testObserver = TestObserver<Result<ByteArray>>()
        val password = "test"

        viewModel.passwordToHash(password).subscribe(testObserver)

        testObserver.assertValue { it is DataResult && it.data.contentEquals(Sha3Utils.keccak(password.toByteArray())) }
    }

    @Test
    fun isSamePassword() {
        val testObserver = TestObserver<Result<Boolean>>()
        val password = "test"
        val passwordHash = Sha3Utils.keccak(password.toByteArray())

        viewModel.isSamePassword(passwordHash, password).subscribe(testObserver)

        testObserver.assertResult(DataResult(true))
    }

    @Test
    fun isNotSamePassword() {
        val testObserver = TestObserver<Result<Boolean>>()
        val password = "test"
        val passwordHash = Sha3Utils.keccak("test2".toByteArray())

        viewModel.isSamePassword(passwordHash, password).subscribe(testObserver)

        testObserver.assertResult(DataResult(false))
    }

    private fun createObserver() = TestObserver.create<Result<Intent>>()
}
