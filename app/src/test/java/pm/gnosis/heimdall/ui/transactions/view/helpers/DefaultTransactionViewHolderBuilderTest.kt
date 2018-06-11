package pm.gnosis.heimdall.ui.transactions.view.helpers

import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.AssetTransferViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.GenericTransactionViewHolder
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class DefaultTransactionViewHolderBuilderTest {

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    @Mock
    private lateinit var tokenRepository: TokenRepository

    private lateinit var addressHelper: AddressHelper

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    lateinit var builder: DefaultTransactionViewHolderBuilder

    @Before
    fun setUp() {
        addressHelper = AddressHelper(addressBookRepository, safeRepository)
        builder = DefaultTransactionViewHolderBuilder(addressHelper, tokenRepository)
    }

    @Test
    fun build() {
        TransactionData::class.nestedClasses.filter { it != TransactionData.Companion::class }.forEach {
            TEST_DATA[it]?.apply {
                val observer = TestObserver<TransactionInfoViewHolder>()
                builder.build(TEST_SAFE, transactionData)
                    .subscribe(observer)
                observer
                    .assertValue(check)
                    .assertNoErrors()
                    .assertComplete()
            } ?: throw IllegalStateException("Missing test for ${it.simpleName}")

        }
    }

    private data class TestData(val transactionData: TransactionData, val check: ((TransactionInfoViewHolder) -> Boolean))

    companion object {
        private val TEST_SAFE = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        private val TEST_ADDRESS = "0xc257274276a4e539741ca11b590b9447b26a8051".asEthereumAddress()!!
        private val TEST_ETHER_TOKEN = Solidity.Address(BigInteger.ZERO)
        private val TEST_ETH_AMOUNT = Wei.ether("23").value

        private val TEST_DATA = mapOf(
            TransactionData.Generic::class to
                    TestData(TransactionData.Generic(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, null), { it is GenericTransactionViewHolder }),
            TransactionData.AssetTransfer::class to
                    TestData(TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS), { it is AssetTransferViewHolder })
        )
    }
}
