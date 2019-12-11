package pm.gnosis.heimdall.ui.transactions.view.helpers

import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.view.TransactionInfoViewHolder
import pm.gnosis.heimdall.ui.transactions.view.viewholders.*
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class DefaultTransactionViewHolderBuilderTest {

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var gnosisSafeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var tokenRepository: TokenRepository

    private lateinit var addressHelper: AddressHelper

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    private lateinit var builder: DefaultTransactionViewHolderBuilder

    @Before
    fun setUp() {
        addressHelper = AddressHelper(addressBookRepository)
        builder = DefaultTransactionViewHolderBuilder(addressHelper, gnosisSafeRepositoryMock, tokenRepository)
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

        private const val REPLACE_RECOVERY_PHRASE_DATA =
            "0x8d80ff0a" + // Multi send method
                    "0000000000000000000000000000000000000000000000000000000000000020" +
                    "0000000000000000000000000000000000000000000000000000000000000240" +
                    "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                    "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000080" +
                    "0000000000000000000000000000000000000000000000000000000000000064" +
                    "e318b52b" + // Swap owner method
                    "000000000000000000000000000000000000000000000000000000000000000c" + // Previous Owner
                    "000000000000000000000000000000000000000000000000000000000000000d" + // Old Owner
                    "000000000000000000000000000000000000000000000000000000000000000f" + // New Owner
                    "00000000000000000000000000000000000000000000000000000000" + // Padding
                    "0000000000000000000000000000000000000000000000000000000000000000" + // Operation
                    "0000000000000000000000001f81fff89bd57811983a35650296681f99c65c7e" + // Safe address
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000080" +
                    "0000000000000000000000000000000000000000000000000000000000000064" +
                    "e318b52b" + // Swap owner method
                    "0000000000000000000000000000000000000000000000000000000000000001" + // Previous Owner
                    "000000000000000000000000000000000000000000000000000000000000000a" + // Old Owner
                    "000000000000000000000000000000000000000000000000000000000000000e" + // New Owner
                    "00000000000000000000000000000000000000000000000000000000" // Padding

        private val REPLACE_RECOVERY_PHRASE_TX =
            SafeTransaction(
                Transaction(
                    address = TEST_SAFE,
                    value = Wei.ZERO,
                    data = REPLACE_RECOVERY_PHRASE_DATA,
                    nonce = BigInteger.ZERO
                ), TransactionExecutionRepository.Operation.DELEGATE_CALL
            )

        private val TEST_DATA = mapOf(
            TransactionData.Generic::class to
                    TestData(TransactionData.Generic(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, null)) { it is GenericTransactionViewHolder },
            TransactionData.AssetTransfer::class to
                    TestData(TransactionData.AssetTransfer(TEST_ETHER_TOKEN, TEST_ETH_AMOUNT, TEST_ADDRESS)) { it is AssetTransferViewHolder },
            TransactionData.ReplaceRecoveryPhrase::class to
                    TestData(TransactionData.ReplaceRecoveryPhrase(REPLACE_RECOVERY_PHRASE_TX)) { it is ReplaceRecoveryPhraseViewHolder },
            TransactionData.ConnectAuthenticator::class to
                    TestData(TransactionData.ConnectAuthenticator(TEST_ADDRESS)) { it is ConnectAuthenticatorViewHolder },
            TransactionData.UpdateMasterCopy::class to
                    TestData(TransactionData.UpdateMasterCopy(TEST_ADDRESS)) { it is UpdateMasterCopyViewHolder },
            TransactionData.MultiSend::class to
                    TestData(TransactionData.MultiSend(emptyList(), TEST_ADDRESS)) { it is MultiSendViewHolder }
        )
    }
}
