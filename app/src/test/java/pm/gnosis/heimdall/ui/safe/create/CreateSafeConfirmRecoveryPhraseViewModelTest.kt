package pm.gnosis.heimdall.ui.safe.create

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.Predicate
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
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreation
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationParams
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationTx
import pm.gnosis.heimdall.data.remote.models.push.ServiceSignature
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.accounts.utils.hash
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexToByteArray
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

    private fun generateSafeCreationData(owners: List<Solidity.Address>, payment: BigInteger = BigInteger.ZERO): String {
        val safeSetup = GnosisSafe.Setup.encode(
            _owners = SolidityBase.Vector(owners),
            _threshold = Solidity.UInt256(if (owners.size == 3) BigInteger.ONE else 2.toBigInteger()),
            to = Solidity.Address(BigInteger.ZERO),
            data = Solidity.Bytes(byteArrayOf())
        ) + "0000000000000000000000000000000000000000000000000000000000000000"
        val expectedConstructor = SolidityBase.encodeTuple(listOf(
            BuildConfig.SAFE_MASTER_COPY_ADDRESS.asEthereumAddress()!!, Solidity.Bytes(safeSetup.hexToByteArray()),
            BuildConfig.SAFE_CREATION_FUNDER.asEthereumAddress()!!, ERC20Token.ETHER_TOKEN.address, Solidity.UInt256(payment)
        ))
        return "0x608060405234801561001057600080fd5b5060405161060a38038061060a833981018060405281019080805190602001909291908051820192919060200180519060200190929190805190602001909291908051906020019092919050505084848160008173ffffffffffffffffffffffffffffffffffffffff1614151515610116576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f496e76616c6964206d617374657220636f707920616464726573732070726f7681526020017f696465640000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506000815111156101a35773ffffffffffffffffffffffffffffffffffffffff60005416600080835160208501846127105a03f46040513d6000823e600082141561019f573d81fd5b5050505b5050600081111561036d57600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156102b7578273ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f1935050505015156102b2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f206574686572000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61036c565b6102d1828483610377640100000000026401000000009004565b151561036b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f20746f6b656e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b5b5b5050505050610490565b600060608383604051602401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506040516020818303038152906040527fa9059cbb000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505090506000808251602084016000896127105a03f16040513d6000823e3d60008114610473576020811461047b5760009450610485565b829450610485565b8151158315171594505b505050509392505050565b61016b8061049f6000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634555d5c91461008b5780635c60da1b146100b6575b73ffffffffffffffffffffffffffffffffffffffff600054163660008037600080366000845af43d6000803e6000811415610086573d6000fd5b3d6000f35b34801561009757600080fd5b506100a061010d565b6040518082815260200191505060405180910390f35b3480156100c257600080fd5b506100cb610116565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b60006002905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050905600a165627a7a7230582007fffd557dfc8c4d2fdf56ba6381a6ce5b65b6260e1492d87f26c6d4f1d041080029" +
                expectedConstructor
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
            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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
                    data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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
            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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
                    data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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
            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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
                    data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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
            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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
                    data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
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

    private fun testSafeCreationChecks(
        account: Account,
        mnemonicAddress0: Solidity.Address,
        mnemonicAddress1: Solidity.Address,
        chromeExtensionAddress: Solidity.Address?,
        responseAnswer: (RelaySafeCreationParams) -> Single<RelaySafeCreation>,
        failurePredicate: Predicate<Throwable>
    ) {
        val testObserver = TestObserver.create<Solidity.Address>()
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willReturn(mnemonicSeed)
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(0L))).willReturn(Single.just(mnemonicAddress0 to mnemonicSeed))
        given(accountsRepositoryMock.accountFromMnemonicSeed(MockUtils.any(), eq(1L))).willReturn(Single.just(mnemonicAddress1 to mnemonicSeed))
        given(relayServiceApiMock.safeCreation(MockUtils.any())).willAnswer {
            val request = it.arguments.first() as RelaySafeCreationParams
            s = request.s
            responseAnswer(request)
        }

        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(
                listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1),
                chromeExtensionAddress?.run { 2 } ?: 1, s)
            )
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(failurePredicate)
    }

    @Test
    fun createSafeDifferentS() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s + 1.toBigInteger(), v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger.TEN,
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei.ZERO
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Client provided parameter s does not match the one returned by the service"
            }
        )
    }

    @Test
    fun createSafeTransactionWithValue() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ether("1"),
                            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger.TEN,
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei.ZERO
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Creation transaction should not require value"
            }
        )
    }

    @Test
    fun createSafeTransactionWithNonZeroNonce() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger.TEN,
                            nonce = BigInteger.ONE
                        ),
                        safeAddress,
                        payment = Wei.ZERO
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Creation transaction should have nonce zero"
            }
        )
    }

    @Test
    fun createSafeTransactionWithTooHighGas() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = generateSafeCreationData(listOfNotNull(account.address, chromeExtensionAddress, mnemonicAddress0, mnemonicAddress1)),
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger("200000000001"), // 201 GWei
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei.ZERO
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Creation transaction should not have a gasPrice higher than 200000000000"
            }
        )
    }

    @Test
    fun createSafeTransactionWithWrongMasterCopy() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = "0x608060405234801561001057600080fd5b5060405161060a38038061060a833981018060405281019080805190602001909291908051820192919060200180519060200190929190805190602001909291908051906020019092919050505084848160008173ffffffffffffffffffffffffffffffffffffffff1614151515610116576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f496e76616c6964206d617374657220636f707920616464726573732070726f7681526020017f696465640000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506000815111156101a35773ffffffffffffffffffffffffffffffffffffffff60005416600080835160208501846127105a03f46040513d6000823e600082141561019f573d81fd5b5050505b5050600081111561036d57600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156102b7578273ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f1935050505015156102b2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f206574686572000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61036c565b6102d1828483610377640100000000026401000000009004565b151561036b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f20746f6b656e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b5b5b5050505050610490565b600060608383604051602401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506040516020818303038152906040527fa9059cbb000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505090506000808251602084016000896127105a03f16040513d6000823e3d60008114610473576020811461047b5760009450610485565b829450610485565b8151158315171594505b505050509392505050565b61016b8061049f6000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634555d5c91461008b5780635c60da1b146100b6575b73ffffffffffffffffffffffffffffffffffffffff600054163660008037600080366000845af43d6000803e6000811415610086573d6000fd5b3d6000f35b34801561009757600080fd5b506100a061010d565b6040518082815260200191505060405180910390f35b3480156100c257600080fd5b506100cb610116565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b60006002905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050905600a165627a7a7230582007fffd557dfc8c4d2fdf56ba6381a6ce5b65b6260e1492d87f26c6d4f1d0410800290000000000000000000000002727d69c0bd14b1ddd28371b8d97e808adc1c2f700000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000d9e09beaeb338d81a7c5688358df0071d498811500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001ed22a342e50000000000000000000000000000000000000000000000000000000000000001440ec78d9e000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000030000000000000000000000001fa2f10de320607934497cee9ffcd14b9d84486d00000000000000000000000029f4cdb73b6b030fa44d05715ac968d12fbc9f72000000000000000000000000a45e212a49f368babecaf5db7e24005ece8a76170000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger("200000000000"), // 200 GWei
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei.ZERO
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected proxy constructor data"
            }
        )
    }

    @Test
    fun createSafeTransactionWithWrongOwner() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = "0x608060405234801561001057600080fd5b5060405161060a38038061060a833981018060405281019080805190602001909291908051820192919060200180519060200190929190805190602001909291908051906020019092919050505084848160008173ffffffffffffffffffffffffffffffffffffffff1614151515610116576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f496e76616c6964206d617374657220636f707920616464726573732070726f7681526020017f696465640000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506000815111156101a35773ffffffffffffffffffffffffffffffffffffffff60005416600080835160208501846127105a03f46040513d6000823e600082141561019f573d81fd5b5050505b5050600081111561036d57600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156102b7578273ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f1935050505015156102b2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f206574686572000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61036c565b6102d1828483610377640100000000026401000000009004565b151561036b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f20746f6b656e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b5b5b5050505050610490565b600060608383604051602401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506040516020818303038152906040527fa9059cbb000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505090506000808251602084016000896127105a03f16040513d6000823e3d60008114610473576020811461047b5760009450610485565b829450610485565b8151158315171594505b505050509392505050565b61016b8061049f6000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634555d5c91461008b5780635c60da1b146100b6575b73ffffffffffffffffffffffffffffffffffffffff600054163660008037600080366000845af43d6000803e6000811415610086573d6000fd5b3d6000f35b34801561009757600080fd5b506100a061010d565b6040518082815260200191505060405180910390f35b3480156100c257600080fd5b506100cb610116565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b60006002905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050905600a165627a7a7230582007fffd557dfc8c4d2fdf56ba6381a6ce5b65b6260e1492d87f26c6d4f1d0410800290000000000000000000000002727d69c0bd14b1ddd28371b8d97e808adc1c2f700000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000ab8c18e66135561676f0781555d05cf6b22024a300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001ed22a342e50000000000000000000000000000000000000000000000000000000000000001440ec78d9e000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000030000000000000000000000001fa2f10de320607934497cee9ffcd14b9d84486e00000000000000000000000029f4cdb73b6b030fa44d05715ac968d12fbc9f72000000000000000000000000a45e212a49f368babecaf5db7e24005ece8a76170000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger("200000000000"), // 200 GWei
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei.ZERO
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected proxy constructor data"
            }
        )
    }

    @Test
    fun createSafeTransactionWithWrongThreshold() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = "0x608060405234801561001057600080fd5b5060405161060a38038061060a833981018060405281019080805190602001909291908051820192919060200180519060200190929190805190602001909291908051906020019092919050505084848160008173ffffffffffffffffffffffffffffffffffffffff1614151515610116576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f496e76616c6964206d617374657220636f707920616464726573732070726f7681526020017f696465640000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506000815111156101a35773ffffffffffffffffffffffffffffffffffffffff60005416600080835160208501846127105a03f46040513d6000823e600082141561019f573d81fd5b5050505b5050600081111561036d57600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156102b7578273ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f1935050505015156102b2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f206574686572000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61036c565b6102d1828483610377640100000000026401000000009004565b151561036b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f20746f6b656e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b5b5b5050505050610490565b600060608383604051602401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506040516020818303038152906040527fa9059cbb000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505090506000808251602084016000896127105a03f16040513d6000823e3d60008114610473576020811461047b5760009450610485565b829450610485565b8151158315171594505b505050509392505050565b61016b8061049f6000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634555d5c91461008b5780635c60da1b146100b6575b73ffffffffffffffffffffffffffffffffffffffff600054163660008037600080366000845af43d6000803e6000811415610086573d6000fd5b3d6000f35b34801561009757600080fd5b506100a061010d565b6040518082815260200191505060405180910390f35b3480156100c257600080fd5b506100cb610116565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b60006002905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050905600a165627a7a7230582007fffd557dfc8c4d2fdf56ba6381a6ce5b65b6260e1492d87f26c6d4f1d0410800290000000000000000000000002727d69c0bd14b1ddd28371b8d97e808adc1c2f700000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000ab8c18e66135561676f0781555d05cf6b22024a300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001ed22a342e50000000000000000000000000000000000000000000000000000000000000001440ec78d9e000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000030000000000000000000000001fa2f10de320607934497cee9ffcd14b9d84486d00000000000000000000000029f4cdb73b6b030fa44d05715ac968d12fbc9f72000000000000000000000000a45e212a49f368babecaf5db7e24005ece8a76170000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger("200000000000"), // 200 GWei
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei.ZERO
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected proxy constructor data"
            }
        )
    }

    @Test
    fun createSafeTransactionWithTooShortInitData() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = "0x608060405234801561001057600080fd5b5061060a833981018060405281019080805190602001909291908051820192919060200180519060200190929190805190602001909291908051906020019092919050505084848160008173ffffffffffffffffffffffffffffffffffffffff1614151515610116576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f496e76616c6964206d617374657220636f707920616464726573732070726f7681526020017f696465640000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506000815111156101a35773ffffffffffffffffffffffffffffffffffffffff60005416600080835160208501846127105a03f46040513d6000823e600082141561019f573d81fd5b5050505b5050600081111561036d57600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156102b7578273ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f1935050505015156102b2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f206574686572000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61036c565b6102d1828483610377640100000000026401000000009004565b151561036b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f20746f6b656e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b5b5b5050505050610490565b600060608383604051602401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506040516020818303038152906040527fa9059cbb000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505090506000808251602084016000896127105a03f16040513d6000823e3d60008114610473576020811461047b5760009450610485565b829450610485565b8151158315171594505b505050509392505050565b61016b8061049f6000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634555d5c91461008b5780635c60da1b146100b6575b73ffffffffffffffffffffffffffffffffffffffff600054163660008037600080366000845af43d6000803e6000811415610086573d6000fd5b3d6000f35b34801561009757600080fd5b506100a061010d565b6040518082815260200191505060405180910390f35b3480156100c257600080fd5b506100cb610116565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b60006002905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050905600a165627a7a7230582007fffd557dfc8c4d2fdf56ba6381a6ce5b65b6260e1492d87f26c6d4f1d0410801290000000000000000000000002727d69c0bd14b1ddd28371b8d97e808adc1c2f700000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000ab8c18e66135561676f0781555d05cf6b22024a300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001ed22a342e50000000000000000000000000000000000000000000000000000000000000001440ec78d9e000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000030000000000000000000000001fa2f10de320607934497cee9ffcd14b9d84486d00000000000000000000000029f4cdb73b6b030fa44d05715ac968d12fbc9f72000000000000000000000000a45e212a49f368babecaf5db7e24005ece8a76170000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger("200000000000"), // 200 GWei
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei("0x1ed22a342e500".hexAsBigInteger())
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected proxy init data"
            }
        )
    }

    @Test
    fun createSafeTransactionWithWrongInitData() {
        val safeAddress = "ffff".asEthereumAddress()!!
        val account = Account("0x1fa2f10de320607934497cee9ffcd14b9d84486d".asEthereumAddress()!!)
        val mnemonicAddress0 = "0x29f4cdb73b6b030fa44d05715ac968d12fbc9f72".asEthereumAddress()!!
        val mnemonicAddress1 = "0xa45e212a49f368babecaf5db7e24005ece8a7617".asEthereumAddress()!!
        val chromeExtensionAddress = null
        testSafeCreationChecks(
            account, mnemonicAddress0, mnemonicAddress1, chromeExtensionAddress,
            { request ->
                Single.just(
                    RelaySafeCreation(
                        ServiceSignature(r = BigInteger.ZERO, s = request.s, v = 27),
                        RelaySafeCreationTx(
                            from = Solidity.Address(1234.toBigInteger()),
                            value = Wei.ZERO,
                            data = "0x608060405234801561001057600080fd5b5060405161060a38038061060a833981018060405281019080805190602001909291908051820192919060200180519060200190929190805190602001909291908051906020019092919050505084848160008173ffffffffffffffffffffffffffffffffffffffff1614151515610116576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001807f496e76616c6964206d617374657220636f707920616464726573732070726f7681526020017f696465640000000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff160217905550506000815111156101a35773ffffffffffffffffffffffffffffffffffffffff60005416600080835160208501846127105a03f46040513d6000823e600082141561019f573d81fd5b5050505b5050600081111561036d57600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156102b7578273ffffffffffffffffffffffffffffffffffffffff166108fc829081150290604051600060405180830381858888f1935050505015156102b2576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f206574686572000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b61036c565b6102d1828483610377640100000000026401000000009004565b151561036b576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260268152602001807f436f756c64206e6f74207061792073616665206372656174696f6e207769746881526020017f20746f6b656e000000000000000000000000000000000000000000000000000081525060400191505060405180910390fd5b5b5b5050505050610490565b600060608383604051602401808373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001828152602001925050506040516020818303038152906040527fa9059cbb000000000000000000000000000000000000000000000000000000007bffffffffffffffffffffffffffffffffffffffffffffffffffffffff19166020820180517bffffffffffffffffffffffffffffffffffffffffffffffffffffffff838183161783525050505090506000808251602084016000896127105a03f16040513d6000823e3d60008114610473576020811461047b5760009450610485565b829450610485565b8151158315171594505b505050509392505050565b61016b8061049f6000396000f30060806040526004361061004c576000357c0100000000000000000000000000000000000000000000000000000000900463ffffffff1680634555d5c91461008b5780635c60da1b146100b6575b73ffffffffffffffffffffffffffffffffffffffff600054163660008037600080366000845af43d6000803e6000811415610086573d6000fd5b3d6000f35b34801561009757600080fd5b506100a061010d565b6040518082815260200191505060405180910390f35b3480156100c257600080fd5b506100cb610116565b604051808273ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200191505060405180910390f35b60006002905090565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff169050905601a165627a7a7230582007fffd557dfc8c4d2fdf56ba6381a6ce5b65b6260e1492d87f26c6d4f1d0410800290000000000000000000000002727d69c0bd14b1ddd28371b8d97e808adc1c2f700000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000ab8c18e66135561676f0781555d05cf6b22024a300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001ed22a342e50000000000000000000000000000000000000000000000000000000000000001440ec78d9e000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000030000000000000000000000001fa2f10de320607934497cee9ffcd14b9d84486d00000000000000000000000029f4cdb73b6b030fa44d05715ac968d12fbc9f72000000000000000000000000a45e212a49f368babecaf5db7e24005ece8a76170000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                            gas = BigInteger.TEN,
                            gasPrice = BigInteger("200000000000"), // 200 GWei
                            nonce = BigInteger.ZERO
                        ),
                        safeAddress,
                        payment = Wei("0x1ed22a342e500".hexAsBigInteger())
                    )
                )
            },
            Predicate { error ->
                error is IllegalStateException && error.message == "Unexpected proxy init data"
            }
        )
    }

    @Test
    fun createSafeWithoutBrowserExtension() {
        val testObserver = TestObserver.create<Solidity.Address>()
        val account = Account(Solidity.Address(10.toBigInteger()))
        val mnemonicAddress0 = Solidity.Address(11.toBigInteger())
        val mnemonicAddress1 = Solidity.Address(12.toBigInteger())
        val safeAddress = "E7B3104C02D70B5E79716FF8683614777DD09D9".asEthereumAddress()!!
        val deployer = Solidity.Address(1234.toBigInteger())
        val mnemonicSeed = byteArrayOf(0)
        var s: BigInteger = BigInteger.ZERO
        val tx = Transaction(
            address = Solidity.Address(BigInteger.ZERO),
            gas = BigInteger.TEN,
            gasPrice = BigInteger.TEN,
            data = generateSafeCreationData(listOfNotNull(account.address, mnemonicAddress0, mnemonicAddress1)),
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
                    data = generateSafeCreationData(listOfNotNull(account.address, mnemonicAddress0, mnemonicAddress1)),
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

        viewModel.setup(null)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(relayServiceApiMock).should()
            .safeCreation(RelaySafeCreationParams(listOf(account.address, mnemonicAddress0, mnemonicAddress1), 1, s))
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
        viewModel.setup(RECOVERY_PHRASE)
        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 0L)
        then(accountsRepositoryMock).should().accountFromMnemonicSeed(mnemonicSeed, 1L)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun createSafeMnemonicToSeedError() {
        val testObserver = TestObserver.create<Solidity.Address>()
        val account = Account(Solidity.Address(10.toBigInteger()))
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val exception = IllegalArgumentException()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(bip39Mock.mnemonicToSeed(anyString(), MockUtils.any())).willThrow(exception)

        // Setup parent class
        viewModel.setup(RECOVERY_PHRASE)
        viewModel.setup(chromeExtensionAddress)
        viewModel.createSafe().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(bip39Mock).should().mnemonicToSeed(RECOVERY_PHRASE)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(relayServiceApiMock).shouldHaveZeroInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun createSafeLoadActiveAccountError() {
        val testObserver = TestObserver.create<Solidity.Address>()
        val chromeExtensionAddress = Solidity.Address(13.toBigInteger())
        val exception = IllegalArgumentException()

        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error(exception))

        // Setup parent class
        viewModel.setup(RECOVERY_PHRASE)
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
