package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
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
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class CreateSafeConfirmRecoveryPhraseViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    private lateinit var bip39Mock: Bip39

    @Mock
    private lateinit var relayServiceApiMock: RelayServiceApi

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    private lateinit var viewModel: CreateSafeConfirmRecoveryPhraseViewModel

    @Before
    fun setup() {
        viewModel = CreateSafeConfirmRecoveryPhraseViewModel(accountsRepositoryMock, bip39Mock, relayServiceApiMock, gnosisSafeRepositoryMock)
        // Setup parent class
        viewModel.setup(RECOVERY_PHRASE)
    }

    @Test
    fun createSafe() {
        val testObserver = TestObserver.create<Solidity.Address>()
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
            gnosisSafeRepositoryMock.addPendingSafe(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.complete())

        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        val txHash = tx.hash(ECDSASignature(r = response!!.signature.r, s = response!!.signature.s).apply { v = response!!.signature.v.toByte() })
            .asBigInteger()
        then(gnosisSafeRepositoryMock).should().addPendingSafe(response!!.safe, txHash, null, response!!.payment)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()

        testObserver.assertResult(safeAddress)
    }

    @Test
    fun createSafeSaveDbError() {
        val testObserver = TestObserver.create<Solidity.Address>()
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
            gnosisSafeRepositoryMock.addPendingSafe(
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any(),
                MockUtils.any()
            )
        ).willReturn(Completable.error(exception))

        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        val txHash = tx.hash(ECDSASignature(r = response!!.signature.r, s = response!!.signature.s).apply { v = response!!.signature.v.toByte() })
            .asBigInteger()
        then(gnosisSafeRepositoryMock).should().addPendingSafe(response!!.safe, txHash, null, response!!.payment)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun createSafeAddressesNotMatching() {
        val testObserver = TestObserver.create<Solidity.Address>()
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

        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun createSafeAccountRecoveryError() {
        val testObserver = TestObserver.create<Solidity.Address>()
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

        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).should().recover(tx.hash(), response!!.signature.toSignature())
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun createSafeDifferentS() {
        val testObserver = TestObserver.create<Solidity.Address>()
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

        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun createSafeApiError() {
        val testObserver = TestObserver.create<Solidity.Address>()
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

        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1), 2, s))
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun createSafeFromMnemonicSeedError() {
        val recoveryPhrase = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Solidity.Address>()
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val mnemonicSeed = byteArrayOf(0)
        val exception = Exception()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.error(exception))

        // Setup parent class
        viewModel.setup(recoveryPhrase)
        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(recoveryPhrase)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun createSafeMnemonicToSeedError() {
        val recoveryPhrase = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Solidity.Address>()
        val account = Account(Solidity.Address(10.toBigInteger()))
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val exception = IllegalArgumentException()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willThrow(exception)

        // Setup parent class
        viewModel.setup(recoveryPhrase)
        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(recoveryPhrase)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun createSafeLoadActiveAccountError() {
        val recoveryPhrase = "degree media athlete harvest rocket plate minute obey head toward coach senior"
        val testObserver = TestObserver.create<Solidity.Address>()
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val exception = IllegalArgumentException()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error(exception))

        // Setup parent class
        viewModel.setup(recoveryPhrase)
        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveZeroInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertError(exception)
    }

    companion object {
        private const val RECOVERY_PHRASE = "degree media athlete harvest rocket plate minute obey head toward coach senior"
    }
}
