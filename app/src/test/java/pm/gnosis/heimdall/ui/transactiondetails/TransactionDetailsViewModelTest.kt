package pm.gnosis.heimdall.ui.transactiondetails

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
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
import pm.gnosis.heimdall.accounts.base.models.Transaction
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.util.DataResult
import pm.gnosis.heimdall.common.util.Result
import pm.gnosis.heimdall.data.contracts.GnosisMultisigTransaction
import pm.gnosis.heimdall.data.contracts.GnosisMultisigWrapper
import pm.gnosis.heimdall.data.contracts.MultisigAddOwner
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.TransactionDetails
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.test.utils.ImmediateSchedulersRule
import pm.gnosis.heimdall.test.utils.MockUtils
import pm.gnosis.heimdall.test.utils.TestCompletable
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexToByteArray
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
    lateinit var gnosisMultisigWrapperMock: GnosisMultisigWrapper

    lateinit var viewModel: TransactionDetailsViewModel

    private val testAddress = "0x0000000000000000000000000000000000000000"
    private val transactionId = "0000000000000000000000000000000000000000000000000000000000000000"
    private var confirmTransactionData = "0x${MultiSigWalletWithDailyLimit.ConfirmTransaction.METHOD_ID}$transactionId"
    private var revokeTransactionData = "0x${MultiSigWalletWithDailyLimit.RevokeConfirmation.METHOD_ID}$transactionId"

    @Before
    fun setUp() {
        viewModel = TransactionDetailsViewModel(ethereumJsonRpcRepositoryMock, accountsRepositoryMock, multisigRepositoryMock, gnosisMultisigWrapperMock)
    }

    @Test
    fun setConfirmTransaction() {
        val transaction = TransactionDetails(testAddress.hexAsBigInteger(), data = confirmTransactionData)
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(transaction).subscribe(testObserver)

        assertEquals(BigInteger(transactionId), viewModel.getMultisigTransactionId())
        assertTrue(viewModel.getMultisigTransactionType() is ConfirmMultisigTransaction)
        testObserver.assertTerminated().assertNoErrors()
    }

    @Test
    fun setRevokeTransaction() {
        val transaction = TransactionDetails(testAddress.hexAsBigInteger(), data = revokeTransactionData)
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(transaction).subscribe(testObserver)

        assertEquals(BigInteger(transactionId), viewModel.getMultisigTransactionId())
        assertTrue(viewModel.getMultisigTransactionType() is RevokeMultisigTransaction)
        testObserver.assertTerminated().assertNoErrors()
    }

    @Test
    fun setUnknownTransaction() {
        val transaction = TransactionDetails(testAddress.hexAsBigInteger(), data = "")
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(transaction).subscribe(testObserver)

        testObserver
                .assertError { it is IllegalStateException }
                .assertTerminated()
    }

    @Test
    fun setTransactionWithInvalidMultisigAddress() {
        val transaction = TransactionDetails(BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16), data = confirmTransactionData)
        val testObserver = TestObserver<Unit>()

        viewModel.setTransaction(transaction).subscribe(testObserver)

        testObserver
                .assertError { it is IllegalStateException }
                .assertTerminated()
    }

    @Test
    fun getMultisigWalletDetails() {
        val transaction = TransactionDetails(testAddress.hexAsBigInteger(), data = confirmTransactionData)
        viewModel.setTransaction(transaction).subscribe()
        given(multisigRepositoryMock.observeMultisigWallet(anyString())).willReturn(Flowable.just(MultisigWallet(testAddress)))

        viewModel.getMultisigWalletDetails()

        then(multisigRepositoryMock).should().observeMultisigWallet(testAddress)
    }

    @Test
    fun signTransaction() {
        val account = Account(testAddress)
        val transactionDetails = TransactionDetails(testAddress.hexAsBigInteger(), data = confirmTransactionData)
        val transactionCallParams = TransactionCallParams(to = transactionDetails.address.asEthereumAddressString(), data = transactionDetails.data)
        val params = EthereumJsonRpcRepository.TransactionParameters(gas = BigInteger.ZERO, gasPrice = BigInteger.ZERO, nonce = BigInteger.ZERO)
        val transaction = Transaction(nonce = params.nonce,
                gasPrice = params.gasPrice,
                startGas = params.gas,
                to = transactionDetails.address,
                value = transactionDetails.value?.value ?: BigInteger("0"),
                data = transactionDetails.data?.hexToByteArray() ?: ByteArray(0))
        val signedTransaction = "signed transaction"
        val testObserver = TestObserver.create<String>()
        viewModel.setTransaction(transactionDetails).subscribe()
        given(accountsRepositoryMock.loadActiveAccount()).willReturn(Single.just(account))
        given(ethereumJsonRpcRepositoryMock.getTransactionParameters(anyString(), MockUtils.any())).willReturn(Observable.just(params))
        given(accountsRepositoryMock.signTransaction(MockUtils.any())).willReturn(Single.just(signedTransaction))
        given(ethereumJsonRpcRepositoryMock.sendRawTransaction(anyString())).willReturn(Observable.just("hash"))

        viewModel.signTransaction().subscribe(testObserver)

        then(accountsRepositoryMock).should().loadActiveAccount()
        then(ethereumJsonRpcRepositoryMock).should().getTransactionParameters(testAddress, transactionCallParams)
        then(accountsRepositoryMock).should().signTransaction(transaction)
        then(ethereumJsonRpcRepositoryMock).should().sendRawTransaction(signedTransaction)
        testObserver.assertNoErrors().assertValue("hash")
    }

    @Test
    fun addMultisigWallet() {
        val address = testAddress
        val name = "test wallet"
        val addMultisigWalletCompletable = TestCompletable()
        val testObserver = TestObserver<Result<String>>()
        given(multisigRepositoryMock.addMultisigWallet(anyString(), anyString())).willReturn(addMultisigWalletCompletable)

        viewModel.addMultisigWallet(address, name).subscribe(testObserver)

        then(multisigRepositoryMock).should().addMultisigWallet(testAddress, name)
        testObserver.assertValue(DataResult(address)).assertNoErrors()
    }

    @Test
    fun getTransactionDetails() {
        val testObserver = TestObserver<GnosisMultisigTransaction>()
        val addOwnerTransaction = MultisigAddOwner(testAddress.hexAsBigInteger())
        val transactionDetails = TransactionDetails(testAddress.hexAsBigInteger(), data = confirmTransactionData)
        viewModel.setTransaction(transactionDetails).subscribe()
        given(gnosisMultisigWrapperMock.getTransaction(anyString(), MockUtils.any())).willReturn(Observable.just(addOwnerTransaction))

        viewModel.getTransactionDetails().subscribe(testObserver)

        then(gnosisMultisigWrapperMock).should().getTransaction(testAddress, viewModel.getMultisigTransactionId())
        testObserver.assertNoErrors().assertValue(addOwnerTransaction)
    }

    @Test
    fun getTokenInfo() {
        val token = ERC20Token(testAddress)
        val testObserver = TestObserver.create<ERC20Token>()
        given(ethereumJsonRpcRepositoryMock.getTokenInfo(MockUtils.any())).willReturn(Observable.just(token))

        viewModel.getTokenInfo(testAddress.hexAsBigInteger()).subscribe(testObserver)

        then(ethereumJsonRpcRepositoryMock).should().getTokenInfo(testAddress.hexAsBigInteger())
        testObserver.assertValue(token).assertNoErrors().assertTerminated()
    }
}
