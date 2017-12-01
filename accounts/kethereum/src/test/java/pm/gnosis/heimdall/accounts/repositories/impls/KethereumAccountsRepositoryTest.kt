package pm.gnosis.heimdall.accounts.repositories.impls

import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.accounts.data.db.AccountDao
import pm.gnosis.heimdall.accounts.data.db.AccountsDatabase
import pm.gnosis.heimdall.accounts.repositories.impls.models.db.AccountDb
import pm.gnosis.heimdall.accounts.utils.rlp
import pm.gnosis.heimdall.common.PreferencesManager
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.heimdall.security.db.EncryptedByteArray
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.hexAsEthereumAddress
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.toHexString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class KethereumAccountsRepositoryTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var accountsDatabase: AccountsDatabase

    @Mock
    lateinit var encryptionManager: EncryptionManager

    @Mock
    lateinit var preferencesManager: PreferencesManager

    @Mock
    lateinit var bip39: Bip39

    lateinit var accountsDao: AccountDao

    lateinit var repository: KethereumAccountsRepository

    @Before
    fun setup() {
        accountsDao = mock(AccountDao::class.java)
        given(accountsDatabase.accountsDao()).willReturn(accountsDao)
        given(encryptionManager.encrypt(MockUtils.any())).willAnswer { it.arguments.first() }
        given(encryptionManager.decrypt(MockUtils.any())).willAnswer { it.arguments.first() }
        repository = KethereumAccountsRepository(accountsDatabase, encryptionManager, preferencesManager, bip39)
    }

    @Test
    fun signTransactionEIP155Example() {
        /* Node.js code:
        const Tx = require('ethereumjs-tx')
        var tra = {nonce: 9, gasPrice: web3.toHex(20000000000), gasLimit: web3.toHex(21000), data: '', to: '0x3535353535353535353535353535353535353535', chainId: 1, value: 1000000000000000000};
        var tx = new Tx(tra)
        var key = new Buffer('4646464646464646464646464646464646464646464646464646464646464646', 'hex')
        tx.sign(key)
        tx.serialize().toString('hex')
         */
        val privateKey = "0x4646464646464646464646464646464646464646464646464646464646464646"
        val encryptedKey = EncryptedByteArray.create(encryptionManager, privateKey.hexStringToByteArray())
        val account = AccountDb(encryptedKey)
        given(accountsDao.observeAccounts()).willReturn(Single.just(account))
        val testObserver = TestObserver<String>()
        val address = "0x3535353535353535353535353535353535353535".hexAsEthereumAddress()
        val nonce = BigInteger("9")
        val value = Wei(BigInteger("1000000000000000000"))
        val gas = BigInteger("21000")
        val gasPrice = BigInteger("20000000000")
        val transaction = Transaction(address = address, nonce = nonce, value = value, gas = gas, gasPrice = gasPrice, chainId = 1)

        repository.signTransaction(transaction).subscribe(testObserver)

        val expectedTx = "0xf86c098504a817c800825208943535353535353535353535353535353535353535880de0b6b3a76400008025a028ef61340bd939bc2195fe537567866003e1a15d3c71ff63e1590620aa636276a067cbe9d8997f761aecb703304b3800ccf555c9f3dc64214b297fb1966a3b6d83"
        testObserver.assertNoErrors().assertValueCount(1)
        assertEquals(expectedTx, testObserver.values().first())
    }

    @Test
    fun rlpTransactionExtension() {
        val address = "0x19fd8863ea1185d8ef7ab3f2a8f4d469dc35dd52".hexAsEthereumAddress()
        val nonce = BigInteger("13")
        val gas = BigInteger("2034776")
        val gasPrice = BigInteger("20000000000")
        val transaction = Transaction(address = address, nonce = nonce, data = "0xe411526d", gas = gas, gasPrice = gasPrice, chainId = 28)
        val expectedString = "e90d8504a817c800831f0c589419fd8863ea1185d8ef7ab3f2a8f4d469dc35dd528084e411526d1c8080"
        assertEquals(expectedString, transaction.rlp().toHexString())
    }

    @Test
    fun signTransactionCreateSafe() {
        /* Node.js code:
        const Tx = require('ethereumjs-tx')
        var tra = {nonce: 13, gasPrice: web3.toHex(20000000000), gasLimit: web3.toHex(2034776), data: "0xe411526d000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000a5056c8efadb5d6a1a6eb0176615692b6e648313", to: '0x19fd8863ea1185d8ef7ab3f2a8f4d469dc35dd52', chainId: 0};
        var tx = new Tx(tra)
        var key = new Buffer('8678adf78db8d1c8a40028795077b3463ca06a743ca37dfd28a5b4442c27b457', 'hex')
        tx.sign(key)
        tx.serialize().toString('hex')
         */
        val privateKey = "0x8678adf78db8d1c8a40028795077b3463ca06a743ca37dfd28a5b4442c27b457"
        val encryptedKey = EncryptedByteArray.create(encryptionManager, privateKey.hexStringToByteArray())
        val account = AccountDb(encryptedKey)
        given(accountsDao.observeAccounts()).willReturn(Single.just(account))
        val testObserver = TestObserver<String>()
        val address = "0x19fd8863ea1185d8ef7ab3f2a8f4d469dc35dd52".hexAsEthereumAddress()
        val nonce = BigInteger("13")
        val gas = BigInteger("2034776")
        val gasPrice = BigInteger("20000000000")
        val transaction = Transaction(address = address, nonce = nonce, data = CREATE_SAFE_DATA, gas = gas, gasPrice = gasPrice, chainId = 0)

        repository.signTransaction(transaction).subscribe(testObserver)

        val expectedTx = "0xf8ea0d8504a817c800831f0c589419fd8863ea1185d8ef7ab3f2a8f4d469dc35dd5280b884e411526d000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000a5056c8efadb5d6a1a6eb0176615692b6e6483131ba067f34dee111e9ab4a034eccb458403b1ee5b80ac801f161d8151f75faa5fbb71a05c88f85eaa2bd540c6d7778787016c27ac92e706dbb19e07c61433d61bf6925f"
        testObserver.assertNoErrors().assertValueCount(1)
        assertEquals(expectedTx, testObserver.values().first())

    }

    companion object {
        const val CREATE_SAFE_DATA = "0x" +
                "e411526d" +
                "0000000000000000000000000000000000000000000000000000000000000040" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "000000000000000000000000a5056c8efadb5d6a1a6eb0176615692b6e648313"
    }

}