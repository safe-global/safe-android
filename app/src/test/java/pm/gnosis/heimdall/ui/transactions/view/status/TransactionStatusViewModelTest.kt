package pm.gnosis.heimdall.ui.transactions.view.status

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.helpers.TransactionViewHolderBuilder
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.*
import kotlin.NoSuchElementException

@RunWith(MockitoJUnitRunner::class)
class TransactionStatusViewModelTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var infoRepositoryMock: TransactionInfoRepository

    @Mock
    private lateinit var executionRepositoryMock: TransactionExecutionRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var viewHolderBuilderMock: TransactionViewHolderBuilder

    @Mock
    private lateinit var transactionViewHolder: TransactionInfoViewHolder

    private lateinit var viewModel: TransactionStatusViewModel

    @Before
    fun setUp() {
        given(viewHolderBuilderMock.build(MockUtils.any(), MockUtils.any(), anyBoolean()))
            .willReturn(Single.just(transactionViewHolder))
        viewModel = TransactionStatusViewModel(infoRepositoryMock, executionRepositoryMock, tokenRepositoryMock, viewHolderBuilderMock)
    }

    private fun testObserve(
        gasTokenAddress: Solidity.Address,
        gasToken: Result<ERC20Token>,
        transactionData: TransactionData,
        type: Int
    ) {

        val observer = TestObserver<TransactionStatusContract.ViewUpdate>()
        given(infoRepositoryMock.loadTransactionInfo(anyString()))
            .willReturn(
                Single.just(
                    TransactionInfo(
                        TEST_ID,
                        TEST_CHAIN_HASH,
                        TEST_SAFE,
                        transactionData,
                        TEST_TIMESTAMP,
                        TEST_GAS_LIMIT,
                        TEST_GAS_PRICE,
                        gasTokenAddress
                    )
                )
            )

        gasToken.let {
            when (it) {
                is DataResult ->
                    given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.just(it.data))
                is ErrorResult ->
                    given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.error(it.error))
            }
        }

        viewModel.observeUpdates(TEST_ID).subscribe(observer)

        val expectedGasToken = when (gasToken) {
            is DataResult -> gasToken.data
            is ErrorResult -> ERC20Token(gasTokenAddress, "", "", decimals = 0)
        }
        observer.assertResult(
            TransactionStatusContract.ViewUpdate.Params(
                TEST_CHAIN_HASH,
                TEST_TIMESTAMP,
                TEST_GAS_LIMIT * TEST_GAS_PRICE,
                expectedGasToken,
                type
            ),
            TransactionStatusContract.ViewUpdate.Details(
                transactionViewHolder
            )
        )

        then(tokenRepositoryMock).should().loadToken(expectedGasToken.address)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()

        then(infoRepositoryMock).should().loadTransactionInfo(TEST_ID)
        then(infoRepositoryMock).shouldHaveNoMoreInteractions()

        then(viewHolderBuilderMock).should().build(TEST_SAFE, transactionData, false)
        then(viewHolderBuilderMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun observeUpdateAssetTransferEtherGasToken() {
        val transactionData = TransactionData.AssetTransfer(TEST_GAS_TOKEN.address, BigInteger.TEN, TEST_SAFE)
        testObserve(ERC20Token.ETHER_TOKEN.address, DataResult(ERC20Token.ETHER_TOKEN), transactionData, R.string.transaction_type_asset_transfer)
    }

    @Test
    fun observeUpdateAssetTransferCustomGasToken() {
        val transactionData = TransactionData.AssetTransfer(TEST_GAS_TOKEN.address, BigInteger.TEN, TEST_SAFE)
        testObserve(TEST_GAS_TOKEN.address, DataResult(TEST_GAS_TOKEN), transactionData, R.string.transaction_type_asset_transfer)
    }

    @Test
    fun observeUpdateAssetTransferCustomGasTokenError() {
        val transactionData = TransactionData.AssetTransfer(TEST_GAS_TOKEN.address, BigInteger.TEN, TEST_SAFE)
        testObserve(TEST_GAS_TOKEN.address, ErrorResult(NoSuchElementException()), transactionData, R.string.transaction_type_asset_transfer)
    }

    @Test
    fun observeUpdateGenericTransactionEtherGasToken() {
        val transactionData = TransactionData.Generic(TEST_SAFE, BigInteger.TEN, null)
        testObserve(ERC20Token.ETHER_TOKEN.address, DataResult(ERC20Token.ETHER_TOKEN), transactionData, R.string.transaction_type_generic)
    }

    @Test
    fun observeUpdateGenericTransactionCustomGasToken() {
        val transactionData = TransactionData.Generic(TEST_SAFE, BigInteger.TEN, null)
        testObserve(TEST_GAS_TOKEN.address, DataResult(TEST_GAS_TOKEN), transactionData, R.string.transaction_type_generic)
    }

    @Test
    fun observeUpdateGenericTransactionCustomGasTokenError() {
        val transactionData = TransactionData.Generic(TEST_SAFE, BigInteger.TEN, null)
        testObserve(TEST_GAS_TOKEN.address, ErrorResult(NoSuchElementException()), transactionData, R.string.transaction_type_generic)
    }

    @Test
    fun observeReplaceRecoveryPhrase() {
        val transactionData = TransactionData.ReplaceRecoveryPhrase(
            SafeTransaction(
                Transaction(address = TEST_SAFE),
                operation = TransactionExecutionRepository.Operation.DELEGATE_CALL
            )
        )
        testObserve(ERC20Token.ETHER_TOKEN.address, DataResult(ERC20Token.ETHER_TOKEN), transactionData, R.string.settings_change)
    }

    @Test
    fun observeConnectExtension() {
        val transactionData = TransactionData.ConnectAuthenticator("0x42".asEthereumAddress()!!)
        testObserve(ERC20Token.ETHER_TOKEN.address, DataResult(ERC20Token.ETHER_TOKEN), transactionData, R.string.settings_change)
    }

    @Test
    fun observerStatus() {
        val statusObservable = Observable.just<TransactionExecutionRepository.PublishStatus>(TransactionExecutionRepository.PublishStatus.Pending)
        given(executionRepositoryMock.observePublishStatus(anyString())).willReturn(statusObservable)
        assertEquals(statusObservable, viewModel.observeStatus(TEST_ID))
        then(executionRepositoryMock).should().observePublishStatus(TEST_ID)
    }

    companion object {
        private val TEST_ID = UUID.randomUUID().toString()
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private val TEST_TIMESTAMP = System.currentTimeMillis()
        private val TEST_GAS_LIMIT = BigInteger.valueOf(100000)
        private val TEST_GAS_PRICE = BigInteger.valueOf(20000000000)
        private val TEST_GAS_TOKEN = ERC20Token(
            "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!,
            name = "Gas Token",
            symbol = "GT",
            decimals = 6
        )
        private const val TEST_CHAIN_HASH = "SomeHash"
    }
}
