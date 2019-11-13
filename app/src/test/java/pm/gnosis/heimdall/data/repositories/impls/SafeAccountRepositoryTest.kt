package pm.gnosis.heimdall.data.repositories.impls

import androidx.room.EmptyResultSetException
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito.*
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.daos.GnosisSafeDao
import pm.gnosis.heimdall.data.db.models.GnosisSafeInfoDb
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.data.db.AccountDao
import pm.gnosis.svalinn.accounts.data.db.AccountsDatabase
import pm.gnosis.svalinn.accounts.repositories.impls.models.db.AccountDb
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.svalinn.security.db.EncryptedByteArray
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.capture
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class SafeAccountRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var appDbMock: ApplicationDb

    @Mock
    lateinit var safeDaoMock: GnosisSafeDao

    @Mock
    lateinit var accountDbMock: AccountsDatabase

    @Mock
    lateinit var accountsDaoMock: AccountDao

    @Mock
    lateinit var bip39Mock: Bip39

    @Mock
    lateinit var cryptoHelperMock: CryptoHelper

    @Mock
    lateinit var encryptionManagerMock: EncryptionManager

    @Captor
    private lateinit var cryptoDataArgumentCaptor: ArgumentCaptor<EncryptionManager.CryptoData>

    private lateinit var repository: SafeAccountRepository

    private val encryptedByteArrayConverter = EncryptedByteArray.Converter()

    @Before
    fun setUp() {
        given(appDbMock.gnosisSafeDao()).willReturn(safeDaoMock)
        given(accountDbMock.accountsDao()).willReturn(accountsDaoMock)
        repository = SafeAccountRepository(accountDbMock, appDbMock, bip39Mock, cryptoHelperMock, encryptionManagerMock)
    }

    @Test
    fun owners() {
        val safe = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val safeAccountAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val safeCryptoData = EncryptionManager.CryptoData.fromString("dead####beef")
        val safeEncryptedKey = encryptedByteArrayConverter.fromStorage(safeCryptoData.toString())
        given(safeDaoMock.loadSafeInfos()).willReturn(Single.just(listOf(buildGnosisSafeInfoDb(safe, safeAccountAddress, safeEncryptedKey))))
        val defaultAccountAddress = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val defaultCryptoData = EncryptionManager.CryptoData.fromString("feed####beef")
        val defaultEncryptedKey = encryptedByteArrayConverter.fromStorage(defaultCryptoData.toString())
        given(accountsDaoMock.observeAccounts()).willReturn(Single.just(AccountDb(defaultEncryptedKey, defaultAccountAddress)))

        val testObserver = TestObserver<List<AccountsRepository.SafeOwner>>()
        repository.owners().subscribe(testObserver)
        testObserver
            .assertSubscribed()
            .assertValueCount(1)
            .assertValue {
                it.size == 2 &&
                        checkOwner(safeAccountAddress, safeCryptoData, it[0]) &&
                        checkOwner(defaultAccountAddress, defaultCryptoData, it[1])
            }
            .assertNoErrors()
            .assertComplete()

        then(safeDaoMock).should().loadSafeInfos()
        then(safeDaoMock).shouldHaveNoMoreInteractions()
        then(accountsDaoMock).should().observeAccounts()
        then(accountsDaoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun ownersNoDeault() {
        val safe1 = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val safe1AccountAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val safe1CryptoData = EncryptionManager.CryptoData.fromString("dead####beef")
        val safe1EncryptedKey = encryptedByteArrayConverter.fromStorage(safe1CryptoData.toString())
        val safe2 = "0x26063CDb1E4AEE199A453857F5174B884a765B7C".asEthereumAddress()!!
        val safe2AccountAddress = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val safe2CryptoData = EncryptionManager.CryptoData.fromString("feed####beef")
        val safe2EncryptedKey = encryptedByteArrayConverter.fromStorage(safe2CryptoData.toString())
        given(safeDaoMock.loadSafeInfos()).willReturn(
            Single.just(
                listOf(
                    buildGnosisSafeInfoDb(safe1, safe1AccountAddress, safe1EncryptedKey),
                    buildGnosisSafeInfoDb(safe2, safe2AccountAddress, safe2EncryptedKey)
                )
            )
        )
        given(accountsDaoMock.observeAccounts()).willReturn(Single.error(EmptyResultSetException("No default account")))

        val testObserver = TestObserver<List<AccountsRepository.SafeOwner>>()
        repository.owners().subscribe(testObserver)
        testObserver
            .assertSubscribed()
            .assertValueCount(1)
            .assertValue {
                it.size == 2 &&
                        checkOwner(safe1AccountAddress, safe1CryptoData, it[0]) &&
                        checkOwner(safe2AccountAddress, safe2CryptoData, it[1])
            }
            .assertNoErrors()
            .assertComplete()

        then(safeDaoMock).should().loadSafeInfos()
        then(safeDaoMock).shouldHaveNoMoreInteractions()
        then(accountsDaoMock).should().observeAccounts()
        then(accountsDaoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun ownersNoAccounts() {
        given(safeDaoMock.loadSafeInfos()).willReturn(Single.just(listOf()))
        given(accountsDaoMock.observeAccounts()).willReturn(Single.error(EmptyResultSetException("No default account")))

        val testObserver = TestObserver<List<AccountsRepository.SafeOwner>>()
        repository.owners().subscribe(testObserver)
        testObserver
            .assertSubscribed()
            .assertValueCount(1)
            .assertValue { it.isEmpty() }
            .assertNoErrors()
            .assertComplete()

        then(safeDaoMock).should().loadSafeInfos()
        then(safeDaoMock).shouldHaveNoMoreInteractions()
        then(accountsDaoMock).should().observeAccounts()
        then(accountsDaoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun createOwner() {
        val encryptedData = EncryptionManager.CryptoData.fromString("dead####beef")
        val mnemonic = "some mnemonic probably not valid"
        val seed =
            "eca9a91e655e12781fd55b433acbd97167344fe025ada1412236411b4097f2d1ac0097a234e893fc24bf7b359e08c5dd0ed1592e227fe212cea0f40e34481deb".hexToByteArray()
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(seed)
        given(bip39Mock.generateMnemonic(anyInt(), anyInt())).willReturn(mnemonic)
        given(encryptionManagerMock.encrypt(MockUtils.any())).willReturn(encryptedData)

        val ownerObserver = TestObserver<AccountsRepository.SafeOwner>()
        repository.createOwner().subscribe(ownerObserver)

        val expectedAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val expectedPrivateKey = "0x10af98b88c193ab5ba5d9c6777fce04081db302fa3d5072a9254fb03fdd90732".hexToByteArray()

        then(bip39Mock).should().generateMnemonic(languageId = R.id.english)
        then(bip39Mock).should().mnemonicToSeed(mnemonic, null)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().encrypt(expectedPrivateKey)
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()

        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(TEST_DECRYPTED_VALUE)
        ownerObserver
            .assertSubscribed()
            .assertValueCount(1)
            .assertValue {
                it.address == expectedAddress &&
                        it.privateKey.value(encryptionManagerMock).contentEquals(TEST_DECRYPTED_VALUE)
            }
            .assertNoErrors()
            .assertComplete()
        then(encryptionManagerMock).should().decrypt(capture(cryptoDataArgumentCaptor))
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        assertEquals(encryptedData.toString(), cryptoDataArgumentCaptor.value.toString())
    }

    @Test
    fun createOwnersFromPhrase() {
        val expectedAddress1 = "0xF48fEF4a24d005A37670d1AC015b4C6f08F9B88b".asEthereumAddress()!!
        val expectedPrivateKey1 = "0xc29a74def7239354a656827c8085f304f79a02388fd774365a1f0d92daf10972".hexToByteArray()
        val encryptedData1 = EncryptionManager.CryptoData.fromString("dead####beef")
        val expectedAddress2 = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val expectedPrivateKey2 = "0xd39f03bb6920fc720577a447f77bbef7d4e19990b6a524fcc551dde34a3fc0b2".hexToByteArray()
        val encryptedData2 = EncryptionManager.CryptoData.fromString("dead####feed")

        val mnemonic = "some mnemonic probably not valid"
        val validatedMnemonic = "some mnemonic probably we valided"
        val seed =
            "eca9a91e655e12781fd55b433acbd97167344fe025ada1412236411b4097f2d1ac0097a234e893fc24bf7b359e08c5dd0ed1592e227fe212cea0f40e34481deb".hexToByteArray()
        given(bip39Mock.mnemonicToSeed(MockUtils.any(), MockUtils.any())).willReturn(seed)
        given(bip39Mock.validateMnemonic(MockUtils.any())).willReturn(validatedMnemonic)
        given(encryptionManagerMock.encrypt(MockUtils.eq(expectedPrivateKey1))).willReturn(encryptedData1)
        given(encryptionManagerMock.encrypt(MockUtils.eq(expectedPrivateKey2))).willReturn(encryptedData2)

        val ownerObserver = TestObserver<List<AccountsRepository.SafeOwner>>()
        repository.createOwnersFromPhrase(mnemonic, listOf(1, 2)).subscribe(ownerObserver)

        then(bip39Mock).should().validateMnemonic(mnemonic)
        then(bip39Mock).should().mnemonicToSeed(validatedMnemonic, null)
        then(bip39Mock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().encrypt(expectedPrivateKey1)
        then(encryptionManagerMock).should().encrypt(expectedPrivateKey2)
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()

        ownerObserver
            .assertSubscribed()
            .assertValueCount(1)
            .assertValue {
                it.size == 2 &&
                        checkOwner(expectedAddress1, encryptedData1, it[0]) &&
                        checkOwner(expectedAddress2, encryptedData2, it[1])
            }
            .assertNoErrors()
            .assertComplete()
    }

    private fun checkOwner(
        expectedAddress: Solidity.Address,
        expectedCryptoData: EncryptionManager.CryptoData,
        safeOwner: AccountsRepository.SafeOwner
    ): Boolean {
        assertEquals(expectedCryptoData.toString(), encryptedByteArrayConverter.toStorage(safeOwner.privateKey))
        assertEquals(expectedAddress, safeOwner.address)
        return true
    }

    @Test
    fun saveOwner() {
        val safe = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val ownerAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val encryptedKey = encryptedByteArrayConverter.fromStorage("dead####beef")

        val testObserver = TestObserver<Unit>()
        repository.saveOwner(safe, AccountsRepository.SafeOwner(ownerAddress, encryptedKey), ERC20Token.ETHER_TOKEN).subscribe(testObserver)
        testObserver.assertResult()

        val expectedInfo = buildGnosisSafeInfoDb(safe, ownerAddress, encryptedKey, "")
        then(safeDaoMock).should().insertSafeInfo(expectedInfo)
        then(safeDaoMock).shouldHaveNoMoreInteractions()
        // Check that key is never decrypted
        then(encryptionManagerMock).shouldHaveZeroInteractions()
    }

    @Test
    fun signingOwnerDefaultAccount() {
        val safe = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val accountAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val encryptedKey = encryptedByteArrayConverter.fromStorage("dead####beef")
        given(safeDaoMock.loadSafeInfo(safe)).willReturn(Single.error(EmptyResultSetException("No account for this Safe")))
        given(accountsDaoMock.observeAccounts()).willReturn(Single.just(AccountDb(encryptedKey, accountAddress)))

        val testObserver = TestObserver<AccountsRepository.SafeOwner>()
        repository.signingOwner(safe).subscribe(testObserver)
        testObserver
            .assertSubscribed()
            .assertValueCount(1)
            .assertValue {
                assertEquals("dead####beef", encryptedByteArrayConverter.toStorage(it.privateKey))
                assertEquals(accountAddress, it.address)
                true
            }
            .assertNoErrors()
            .assertComplete()

        then(safeDaoMock).should().loadSafeInfo(safe)
        then(safeDaoMock).shouldHaveNoMoreInteractions()

        then(accountsDaoMock).should().observeAccounts()
        then(accountsDaoMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun signingOwnerSafeAccount() {
        val safe = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val accountAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val encryptedKey = encryptedByteArrayConverter.fromStorage("dead####beef")
        given(safeDaoMock.loadSafeInfo(safe)).willReturn(Single.just(buildGnosisSafeInfoDb(safe, accountAddress, encryptedKey)))

        val testObserver = TestObserver<AccountsRepository.SafeOwner>()
        repository.signingOwner(safe).subscribe(testObserver)
        testObserver
            .assertSubscribed()
            .assertValueCount(1)
            .assertValue {
                assertEquals("dead####beef", encryptedByteArrayConverter.toStorage(it.privateKey))
                assertEquals(accountAddress, it.address)
                true
            }
            .assertNoErrors()
            .assertComplete()

        then(safeDaoMock).should().loadSafeInfo(safe)
        then(safeDaoMock).shouldHaveNoMoreInteractions()
        then(accountsDaoMock).shouldHaveZeroInteractions()
    }

    @Test
    fun signForSafe() {
        val data = Sha3Utils.keccak("Some cool message".toByteArray())
        val safe = "0xa0A6E24f1450F46aB13cFb5F4034EFc54886a278".asEthereumAddress()!!
        val accountAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val encryptedKey = encryptedByteArrayConverter.fromStorage("dead####beef")
        given(safeDaoMock.loadSafeInfo(safe)).willReturn(Single.just(buildGnosisSafeInfoDb(safe, accountAddress, encryptedKey)))
        val privateKey = Sha3Utils.keccak("moooh".toByteArray())
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(privateKey)
        val signature = Signature(BigInteger.valueOf(11), BigInteger.valueOf(5), 27)
        given(cryptoHelperMock.sign(MockUtils.any(), MockUtils.any())).willReturn(signature)

        val testObserver = TestObserver<Signature>()
        repository.sign(safe, data).subscribe(testObserver)
        testObserver.assertResult(signature)

        then(safeDaoMock).should().loadSafeInfo(safe)
        then(safeDaoMock).shouldHaveNoMoreInteractions()
        then(accountsDaoMock).shouldHaveZeroInteractions()

        then(cryptoHelperMock).should().sign(privateKey, data)
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().decrypt(capture(cryptoDataArgumentCaptor))
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        assertEquals("dead####beef", cryptoDataArgumentCaptor.value.toString())
    }

    @Test
    fun signWithOwner() {
        val privateKey = Sha3Utils.keccak("moooh".toByteArray())
        given(encryptionManagerMock.decrypt(MockUtils.any())).willReturn(privateKey)
        val signature = Signature(BigInteger.valueOf(11), BigInteger.valueOf(5), 27)
        given(cryptoHelperMock.sign(MockUtils.any(), MockUtils.any())).willReturn(signature)

        val data = Sha3Utils.keccak("Some cool message".toByteArray())
        val ownerAddress = "0xdb81D06BB45F29FB2F6510A9750d1Dd350378095".asEthereumAddress()!!
        val encryptedKey = encryptedByteArrayConverter.fromStorage("dead####beef")

        val testObserver = TestObserver<Signature>()
        repository.sign(AccountsRepository.SafeOwner(ownerAddress, encryptedKey), data).subscribe(testObserver)
        testObserver.assertResult(signature)

        then(cryptoHelperMock).should().sign(privateKey, data)
        then(cryptoHelperMock).shouldHaveNoMoreInteractions()
        then(encryptionManagerMock).should().decrypt(capture(cryptoDataArgumentCaptor))
        then(encryptionManagerMock).shouldHaveNoMoreInteractions()
        assertEquals("dead####beef", cryptoDataArgumentCaptor.value.toString())
    }

    private fun buildGnosisSafeInfoDb(
        safe: Solidity.Address,
        safeAccountAddress: Solidity.Address,
        safeEncryptedKey: EncryptedByteArray,
        paymentTokenIcon: String? = null
    ) = GnosisSafeInfoDb(
        safe,
        safeAccountAddress,
        safeEncryptedKey,
        ERC20Token.ETHER_TOKEN.address,
        ERC20Token.ETHER_TOKEN.symbol,
        ERC20Token.ETHER_TOKEN.name,
        ERC20Token.ETHER_TOKEN.decimals,
        paymentTokenIcon
    )

    companion object {
        private val TEST_DECRYPTED_VALUE = "some decrypted value".toByteArray()
    }
}
