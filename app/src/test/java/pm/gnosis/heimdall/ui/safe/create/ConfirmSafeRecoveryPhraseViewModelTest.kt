package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.*
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreation
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationParams
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationTx
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.accounts.utils.hash
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.words
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class ConfirmSafeRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var bip39Mock: Bip39

    @Mock
    private lateinit var encryptionManagerMock: EncryptionManager

    @Mock
    private lateinit var relayServiceApiMock: RelayServiceApi

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    @Captor
    private lateinit var cryptoDataCaptor: ArgumentCaptor<EncryptionManager.CryptoData>

    private lateinit var viewModel: ConfirmSafeRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = ConfirmSafeRecoveryPhraseViewModel(
            accountsRepositoryMock,
            bip39Mock,
            encryptionManagerMock,
            relayServiceApiMock,
            gnosisSafeRepositoryMock
        )
    }

    @Test
    fun testSetup() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val encryptedMnemonic = "ffffff####ffffff"
        val testObserver = TestObserver.create<List<String>>()
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, Solidity.Address(1.toBigInteger())).subscribe(testObserver)

        testObserver.assertTerminated().assertNoErrors()
        then(encryptionManagerMock).should().decrypt(capture(cryptoDataCaptor))
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        assertTrue(cryptoDataCaptor.value.data.contentEquals("ffffff".hexStringToByteArray()))
        assertTrue(cryptoDataCaptor.value.iv.contentEquals("ffffff".hexStringToByteArray()))
    }

    @Test
    fun setupEncryptionError() {
        val encryptedMnemonic = "ffffff####ffffff"
        val testObserver = TestObserver.create<List<String>>()
        given(encryptionManagerMock.decrypt(MockUtils.any())).willThrow(IllegalStateException())

        viewModel.setup(encryptedMnemonic, Solidity.Address(1.toBigInteger())).subscribe(testObserver)

        testObserver.assertTerminated().assertFailure(IllegalStateException::class.java)
        then(encryptionManagerMock).should().decrypt(capture(cryptoDataCaptor))
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun setupNot12Words() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach"
        val encryptedMnemonic = "ffffff####ffffff"
        val testObserver = TestObserver.create<List<String>>()
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, Solidity.Address(1.toBigInteger())).subscribe(testObserver)

        testObserver.assertTerminated().assertFailure(IllegalStateException::class.java)
        then(encryptionManagerMock).should().decrypt(capture(cryptoDataCaptor))
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        assertTrue(cryptoDataCaptor.value.data.contentEquals("ffffff".hexStringToByteArray()))
        assertTrue(cryptoDataCaptor.value.iv.contentEquals("ffffff".hexStringToByteArray()))
    }

    @Test
    fun isCorrectSequenceWithCorrectSequence() {
        val testObserver = TestObserver.create<Result<Boolean>>()
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val encryptedMnemonic = "ffffff####ffffff"
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, Solidity.Address(1.toBigInteger())).subscribe()
        viewModel.isCorrectSequence(mnemonic.words()).subscribe(testObserver)

        testObserver.assertResult(DataResult(true))
    }

    @Test
    fun isCorrectSequenceWithIncorrectSequence() {
        val testObserver = TestObserver.create<Result<Boolean>>()
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val incorrectSequence = "degree media athlete harvest rocket plate minute obey head toward coach"
        val encryptedMnemonic = "ffffff####ffffff"
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, Solidity.Address(1.toBigInteger())).subscribe()
        viewModel.isCorrectSequence(incorrectSequence.words()).subscribe(testObserver)

        testObserver.assertResult(DataResult(false))
    }

    @Test
    fun createSafe() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val safeAddress = "E7B3104C02D70B5E79716FF8683614777DD09D9".asEthereumAddress()!!
        val deployer = Solidity.Address(1234.toBigInteger())
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO
        val tx = Transaction(
            address = Solidity.Address(BigInteger.ZERO),
            gas = BigInteger.TEN,
            gasPrice = BigInteger.TEN,
            data = "0x00",
            nonce = BigInteger.ZERO
        )
        var response: RelaySafeCreation? = null

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.just(mnemonicAddress1 to mnemonicSeed))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            s = request.s
            response = RelaySafeCreation(
                ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                RelaySafeCreationTx(
                    from = Solidity.Address(1234.toBigInteger()),
                    value = Wei.ZERO,
                    data = "0x00",
                    gas = BigInteger.TEN,
                    gasPrice = BigInteger.TEN,
                    nonce = BigInteger.ZERO
                ),
                safeAddress,
                payment = Wei.ZERO
            )
            Single.just(response!!)
        }
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.just(deployer))
        given(
            gnosisSafeRepositoryMock.savePendingSafe(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.complete())
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        val txHash = tx.hash(ECDSASignature(r = response!!.signature.r, s = response!!.signature.s).apply { v = response!!.signature.v.toByte() })
            .asBigInteger()
        then(gnosisSafeRepositoryMock).should().savePendingSafe(txHash, null, response!!.safe, response!!.payment)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(DataResult(txHash))
    }

    @Test
    fun createSafeSaveDbError() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val safeAddress = "E7B3104C02D70B5E79716FF8683614777DD09D9".asEthereumAddress()!!
        val deployer = Solidity.Address(1234.toBigInteger())
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO
        val tx = Transaction(
            address = Solidity.Address(BigInteger.ZERO),
            gas = BigInteger.TEN,
            gasPrice = BigInteger.TEN,
            data = "0x00",
            nonce = BigInteger.ZERO
        )
        var response: RelaySafeCreation? = null
        val exception = Exception()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.just(mnemonicAddress1 to mnemonicSeed))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            s = request.s
            response = RelaySafeCreation(
                ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                RelaySafeCreationTx(
                    from = Solidity.Address(1234.toBigInteger()),
                    value = Wei.ZERO,
                    data = "0x00",
                    gas = BigInteger.TEN,
                    gasPrice = BigInteger.TEN,
                    nonce = BigInteger.ZERO
                ),
                safeAddress,
                payment = Wei.ZERO
            )
            Single.just(response!!)
        }
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.just(deployer))
        given(
            gnosisSafeRepositoryMock.savePendingSafe(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.error(exception))
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        val txHash = tx.hash(ECDSASignature(r = response!!.signature.r, s = response!!.signature.s).apply { v = response!!.signature.v.toByte() })
            .asBigInteger()
        then(gnosisSafeRepositoryMock).should().savePendingSafe(txHash, null, response!!.safe, response!!.payment)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun createSafeAddressesNotMatching() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val safeAddress = "ffff".asEthereumAddress()!!
        val deployer = Solidity.Address(1234.toBigInteger())
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO
        val tx = Transaction(
            address = Solidity.Address(BigInteger.ZERO),
            gas = BigInteger.TEN,
            gasPrice = BigInteger.TEN,
            data = "0x00",
            nonce = BigInteger.ZERO
        )
        var response: RelaySafeCreation? = null

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.just(mnemonicAddress1 to mnemonicSeed))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            s = request.s
            response = RelaySafeCreation(
                ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                RelaySafeCreationTx(
                    from = Solidity.Address(1234.toBigInteger()),
                    value = Wei.ZERO,
                    data = "0x00",
                    gas = BigInteger.TEN,
                    gasPrice = BigInteger.TEN,
                    nonce = BigInteger.ZERO
                ),
                safeAddress,
                payment = Wei.ZERO
            )
            Single.just(response!!)
        }
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.just(deployer))
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue { it is ErrorResult && it.error is IllegalStateException }
    }

    @Test
    fun createSafeAccountRecoveryError() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val safeAddress = "ffff".asEthereumAddress()!!
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO
        val tx = Transaction(
            address = Solidity.Address(BigInteger.ZERO),
            gas = BigInteger.TEN,
            gasPrice = BigInteger.TEN,
            data = "0x00",
            nonce = BigInteger.ZERO
        )
        var response: RelaySafeCreation? = null
        val exception = Exception()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.just(mnemonicAddress1 to mnemonicSeed))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            s = request.s
            response = RelaySafeCreation(
                ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                RelaySafeCreationTx(
                    from = Solidity.Address(1234.toBigInteger()),
                    value = Wei.ZERO,
                    data = "0x00",
                    gas = BigInteger.TEN,
                    gasPrice = BigInteger.TEN,
                    nonce = BigInteger.ZERO
                ),
                safeAddress,
                payment = Wei.ZERO
            )
            Single.just(response!!)
        }
        given(accountsRepositoryMock.recover(MockUtils.any(), MockUtils.any())).willReturn(Single.error(exception))
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun createSafeDifferentS() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val safeAddress = "ffff".asEthereumAddress()!!
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.just(mnemonicAddress1 to mnemonicSeed))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            s = request.s
            Single.just(
                RelaySafeCreation(
                    ServiceSignature(r = BigInteger.ZERO, s = request.s + 1.toBigInteger(), v = 27),
                    RelaySafeCreationTx(
                        from = Solidity.Address(1234.toBigInteger()),
                        value = Wei.ZERO,
                        data = "0x00",
                        gas = BigInteger.TEN,
                        gasPrice = BigInteger.TEN,
                        nonce = BigInteger.ZERO
                    ),
                    safeAddress,
                    payment = Wei.ZERO
                )
            )
        }
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue { it is ErrorResult && it.error is IllegalStateException }
    }

    @Test
    fun createSafeApiError() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO
        val exception = Exception()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.just(mnemonicAddress1 to mnemonicSeed))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            s = request.s
            Single.error<RelaySafeCreation>(exception)
        }
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun createSafeFromMnemonicSeedError() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val mnemonicSeed = byteArrayOf(0)
        val exception = Exception()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.error(exception))
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun createSafeMnemonicToSeedError() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val account = Account(Solidity.Address(10.toBigInteger()))
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val exception = IllegalArgumentException()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willThrow(exception)
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(mnemonic)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }

    @Test
    fun createSafeLoadActiveAccountError() {
        val mnemonic = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Result<BigInteger>>()
        val encryptedMnemonic = "ffffff####ffffff"
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val exception = IllegalArgumentException()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error(exception))
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(mnemonic.toByteArray())

        viewModel.setup(encryptedMnemonic, chromeExtensionAddress).subscribe(TestObserver.create())
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveZeroInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertResult(ErrorResult(exception))
    }
}

//TODO extract to test utils
/**
 * Returns ArgumentCaptor.capture() as nullable type to avoid java.lang.IllegalStateException
 * when null is returned.
 */
fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
