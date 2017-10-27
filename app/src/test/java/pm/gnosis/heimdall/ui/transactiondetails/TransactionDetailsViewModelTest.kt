package pm.gnosis.heimdall.ui.transactiondetails

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.utils.DataResult
import pm.gnosis.heimdall.common.utils.ErrorResult
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.remote.models.TransactionParameters
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionDetailRepository
import pm.gnosis.heimdall.data.repositories.impls.GnosisMultisigTransaction
import pm.gnosis.heimdall.data.repositories.impls.MultisigAddOwner
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.models.Transaction
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class TransactionDetailsViewModelTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var ethereumJsonRpcRepositoryMock: EthereumJsonRpcRepository

    @Mock
    lateinit var accountsRepositoryMock: AccountsRepository

    @Mock
    lateinit var multisigRepositoryMock: MultisigRepository

    @Mock
    lateinit var transactionDetailRepositoryMock: TransactionDetailRepository

    @Mock
    lateinit var tokenRepositoryMock: TokenRepository

    lateinit var viewModel: TransactionDetailsViewModel

    private val testAddress = BigInteger.ZERO
    private val transactionId = "0000000000000000000000000000000000000000000000000000000000000000"
    private val confirmTransactionData = "0x${MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID}$transactionId"
    private val revokeTransactionData = "0x${MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID}$transactionId"
    private val testTransaction = Transaction(testAddress, data = confirmTransactionData)

    @Before
    fun setUp() {
        viewModel = TransactionDetailsViewModel(ethereumJsonRpcRepositoryMock, accountsRepositoryMock, multisigRepositoryMock, transactionDetailRepositoryMock, tokenRepositoryMock)
    }

    @Test
    fun setConfirmTransaction() {
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(testTransaction).subscribe(testObserver)

        assertEquals(BigInteger(transactionId), viewModel.getMultisigTransactionId())
        assertTrue(viewModel.getMultisigTransactionType() is ConfirmMultisigTransaction)
        assertEquals(testTransaction, viewModel.getTransaction())
        testObserver.assertTerminated().assertNoErrors()
    }

    @Test
    fun setRevokeTransaction() {
        val transaction = testTransaction.copy(data = revokeTransactionData)
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(transaction).subscribe(testObserver)

        assertEquals(BigInteger(transactionId), viewModel.getMultisigTransactionId())
        assertTrue(viewModel.getMultisigTransactionType() is RevokeMultisigTransaction)
        assertEquals(transaction, viewModel.getTransaction())
        testObserver.assertTerminated().assertNoErrors()
    }

    @Test
    fun setUnknownTransaction() {
        val transaction = testTransaction.copy(data = "")
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(transaction).subscribe(testObserver)

        testObserver
                .assertError { it is IllegalStateException }
                .assertTerminated()
    }

    @Test
    fun setNullTransaction() {
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(null).subscribe(testObserver)

        testObserver
                .assertError { it is IllegalStateException }
                .assertTerminated()
    }

    @Test
    fun setTransactionWithInvalidMultisigAddress() {
        val transaction =  testTransaction.copy(BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16))
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(transaction).subscribe(testObserver)

        testObserver
                .assertError { it is IllegalStateException }
                .assertTerminated()
    }

    @Test
    fun getMultisigWalletDetails() {
        val testObserver = TestSubscriber<MultisigWallet>()
        val wallet = MultisigWallet(testAddress)
        viewModel.setTransaction(testTransaction).subscribe()
        given(multisigRepositoryMock.observeMultisigWallet(MockUtils.any())).willReturn(Flowable.just(wallet))

        viewModel.observeMultisigWalletDetails().subscribe(testObserver)

        then(multisigRepositoryMock).should().observeMultisigWallet(testAddress)
        then(multisigRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(wallet)
    }

    @Test
    fun getMultisigWalletDetailsError() {
        val testObserver = TestSubscriber<MultisigWallet>()
        val exception = Exception()
        viewModel.setTransaction(testTransaction).subscribe()
        given(multisigRepositoryMock.observeMultisigWallet(MockUtils.any())).willReturn(Flowable.error(exception))

        viewModel.observeMultisigWalletDetails().subscribe(testObserver)

        then(multisigRepositoryMock).should().observeMultisigWallet(testAddress)
        then(multisigRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception)
    }

    @Test
    fun signTransaction() {
        val account = Account(testAddress)
        val transactionCallParams = TransactionCallParams(to = testTransaction.address.asEthereumAddressString(), data = testTransaction.data)
        val params = TransactionParameters(gas = BigInteger.ZERO, gasPrice = BigInteger.ZERO, nonce = BigInteger.ZERO)
        val transaction = testTransaction.copy(nonce = params.nonce, gasPrice = params.gasPrice, gas = params.gas)
        val signedTransaction = "signed transaction"
        val dataResult = DataResult("hash")
        val testObserver = TestObserver.create<Result<String>>()
        viewModel.setTransaction(testTransaction).subscribe()
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getTransactionParameters(MockUtils.any(), MockUtils.any())).willReturn(Observable.just(params))
        given(accountsRepositoryMock.signTransaction(MockUtils.any())).willReturn(Single.just(signedTransaction))
        given(ethereumJsonRpcRepositoryMock.sendRawTransaction(anyString())).willReturn(Observable.just("hash"))

        viewModel.signTransaction().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getTransactionParameters(testAddress, transactionCallParams)
        then(accountsRepositoryMock).should().signTransaction(transaction)
        then(ethereumJsonRpcRepositoryMock).should().sendRawTransaction(signedTransaction)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(dataResult)
    }

    @Test
    fun signTransactionErrorLoadAccount() {
        val exception = Exception()
        val errorResult = ErrorResult<String>(exception)
        val testObserver = TestObserver.create<Result<String>>()
        viewModel.setTransaction(testTransaction).subscribe()
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.error(exception))

        viewModel.signTransaction().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(errorResult)
    }

    @Test
    fun signTransactionErrorGetTransactionParameters() {
        val account = Account(testAddress)
        val transactionCallParams = TransactionCallParams(to = testTransaction.address.asEthereumAddressString(), data = testTransaction.data)
        val testObserver = TestObserver.create<Result<String>>()
        val exception = Exception()
        val errorResult = ErrorResult<String>(exception)
        viewModel.setTransaction(testTransaction).subscribe()
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getTransactionParameters(MockUtils.any(), MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.signTransaction().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getTransactionParameters(testAddress, transactionCallParams)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(errorResult)
    }

    @Test
    fun signTransactionErrorSignTransaction() {
        val account = Account(testAddress)
        val transactionCallParams = TransactionCallParams(to = testTransaction.address.asEthereumAddressString(), data = testTransaction.data)
        val params = TransactionParameters(gas = BigInteger.ZERO, gasPrice = BigInteger.ZERO, nonce = BigInteger.ZERO)
        val transaction = testTransaction.copy(nonce = params.nonce, gasPrice = params.gasPrice, gas = params.gas)
        val testObserver = TestObserver.create<Result<String>>()
        val exception = Exception()
        val errorResult = ErrorResult<String>(exception)
        viewModel.setTransaction(testTransaction).subscribe()
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getTransactionParameters(MockUtils.any(), MockUtils.any())).willReturn(Observable.just(params))
        given(accountsRepositoryMock.signTransaction(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.signTransaction().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getTransactionParameters(testAddress, transactionCallParams)
        then(accountsRepositoryMock).should().signTransaction(transaction)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(errorResult)
    }

    @Test
    fun signTransactionErrorSendRawTransaction() {
        val account = Account(testAddress)
        val transactionCallParams = TransactionCallParams(to = testTransaction.address.asEthereumAddressString(), data = testTransaction.data)
        val params = TransactionParameters(gas = BigInteger.ZERO, gasPrice = BigInteger.ZERO, nonce = BigInteger.ZERO)
        val transaction = testTransaction.copy(nonce = params.nonce, gasPrice = params.gasPrice, gas = params.gas)
        val signedTransaction = "signed transaction"
        val testObserver = TestObserver.create<Result<String>>()
        val exception = Exception()
        val errorResult = ErrorResult<String>(exception)
        viewModel.setTransaction(testTransaction).subscribe()
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getTransactionParameters(MockUtils.any(), MockUtils.any())).willReturn(Observable.just(params))
        given(accountsRepositoryMock.signTransaction(MockUtils.any())).willReturn(Single.just(signedTransaction))
        given(ethereumJsonRpcRepositoryMock.sendRawTransaction(anyString())).willReturn(Observable.error(exception))

        viewModel.signTransaction().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getTransactionParameters(testAddress, transactionCallParams)
        then(accountsRepositoryMock).should().signTransaction(transaction)
        then(ethereumJsonRpcRepositoryMock).should().sendRawTransaction(signedTransaction)
        then(accountsRepositoryMock).shouldHaveNoMoreInteractions()
        then(ethereumJsonRpcRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(errorResult)
    }

    @Test
    fun addMultisigWallet() {
        val address = testAddress
        val name = "test wallet"
        val addMultisigWalletCompletable = TestCompletable()
        val testObserver = TestObserver<Result<BigInteger>>()
        given(multisigRepositoryMock.addMultisigWallet(MockUtils.any(), anyString())).willReturn(addMultisigWalletCompletable)

        viewModel.addMultisigWallet(address, name).subscribe(testObserver)

        then(multisigRepositoryMock).should().addMultisigWallet(testAddress, name)
        then(multisigRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(address)).assertNoErrors()
    }

    @Test
    fun addMultisigWalletError() {
        val address = testAddress
        val name = "test wallet"
        val testObserver = TestObserver<Result<BigInteger>>()
        val exception = Exception()
        given(multisigRepositoryMock.addMultisigWallet(MockUtils.any(), anyString())).willReturn(Completable.error(exception))

        viewModel.addMultisigWallet(address, name).subscribe(testObserver)

        then(multisigRepositoryMock).should().addMultisigWallet(testAddress, name)
        then(multisigRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception)).assertNoErrors()
    }

    @Test
    fun getTransaction() {
        val testObserver = TestObserver<GnosisMultisigTransaction>()
        val addOwnerTransaction = MultisigAddOwner(testAddress)
        val transactionDetails = Transaction(testAddress, data = confirmTransactionData)
        viewModel.setTransaction(transactionDetails).subscribe()
        given(transactionDetailRepositoryMock.loadTransactionDetails(MockUtils.any(), MockUtils.any())).willReturn(Observable.just(addOwnerTransaction))

        viewModel.loadTransactionDetails().subscribe(testObserver)

        then(transactionDetailRepositoryMock).should().loadTransactionDetails(testAddress, viewModel.getMultisigTransactionId())
        then(transactionDetailRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoErrors().assertValue(addOwnerTransaction)
    }

    @Test
    fun getTransactionError() {
        val testObserver = TestObserver<GnosisMultisigTransaction>()
        val exception = Exception()
        val transactionDetails = Transaction(testAddress, data = confirmTransactionData)
        viewModel.setTransaction(transactionDetails).subscribe()
        given(transactionDetailRepositoryMock.loadTransactionDetails(MockUtils.any(), MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadTransactionDetails().subscribe(testObserver)

        then(transactionDetailRepositoryMock).should().loadTransactionDetails(testAddress, viewModel.getMultisigTransactionId())
        then(transactionDetailRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertError(exception).assertNoValues()
    }

    @Test
    fun getTokenInfo() {
        val token = ERC20Token(testAddress, decimals = 0)
        val testObserver = TestObserver.create<ERC20Token>()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.just(token))

        viewModel.loadTokenInfo(testAddress).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(token).assertNoErrors().assertTerminated()
    }

    @Test
    fun getTokenInfoError() {
        val testObserver = TestObserver.create<ERC20Token>()
        val exception = Exception()
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(Observable.error(exception))

        viewModel.loadTokenInfo(testAddress).subscribe(testObserver)

        then(tokenRepositoryMock).should().loadTokenInfo(testAddress)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertNoValues().assertError(exception)
    }
}
