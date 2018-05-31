package pm.gnosis.heimdall.ui.transactions.review.viewholders

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.TransactionData
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.transactions.builder.AssetTransferTransactionBuilder
import pm.gnosis.model.Solidity
import pm.gnosis.tests.utils.*
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.concurrent.TimeoutException

@RunWith(MockitoJUnitRunner::class)
class AssetTransferViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var addressBookRepository: AddressBookRepository

    @Mock
    private lateinit var safeRepository: GnosisSafeRepository

    @Mock
    private lateinit var tokenRepositoryMock: TokenRepository

    @Mock
    private lateinit var layoutInflater: LayoutInflater

    @Mock
    private lateinit var containerView: ViewGroup

    @Mock
    private lateinit var viewHolderView: View

    @Mock
    private lateinit var valueView: TextView

    @Mock
    private lateinit var fiatView: TextView

    @Mock
    private lateinit var safeAddressView: TextView

    @Mock
    private lateinit var safeNameView: TextView

    @Mock
    private lateinit var safeImageView: BlockiesImageView

    @Mock
    private lateinit var receiverAddressView: TextView

    @Mock
    private lateinit var receiverNameView: TextView

    @Mock
    private lateinit var receiverImageView: BlockiesImageView

    @Mock
    private lateinit var safeBalanceView: TextView

    private lateinit var addressHelper: AddressHelper

    private lateinit var viewHolder: AssetTransferViewHolder

    @Before
    fun setUp() {
        addressHelper = AddressHelper(addressBookRepository, safeRepository)
    }

    @Test
    fun testNoInteractionWithoutView() {
        val data = TransactionData.AssetTransfer(TEST_TOKEN, TEST_AMOUNT, TEST_RECEIVER)
        viewHolder = AssetTransferViewHolder(TEST_SAFE, data, addressHelper, tokenRepositoryMock)
        viewHolder.start()
        then(contextMock).shouldHaveZeroInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
        then(layoutInflater).shouldHaveZeroInteractions()
    }

    private fun setupViewMocks(balancesObservable: Observable<List<Pair<ERC20Token, BigInteger?>>>) {
        given(tokenRepositoryMock.loadTokenBalances(MockUtils.any(), MockUtils.any())).willReturn(balancesObservable)
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(layoutInflater.inflate(R.layout.layout_asset_transfer_info, containerView, true)).willReturn(viewHolderView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_value, valueView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_fiat, fiatView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_safe_address, safeAddressView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_safe_name, safeNameView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_safe_image, safeImageView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_to_address, receiverAddressView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_to_name, receiverNameView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_to_image, receiverImageView)
        viewHolderView.mockFindViewById(R.id.layout_asset_transfer_info_safe_balance, safeBalanceView)
    }

    private fun assertAddressHelperInteractions() {
        then(safeNameView).should().visibility = View.GONE
        then(safeNameView).shouldHaveNoMoreInteractions()
        then(safeImageView).should().setAddress(TEST_SAFE)
        then(safeImageView).shouldHaveNoMoreInteractions()
        then(safeAddressView).should().text = "0xFe21...5016ee"
        then(safeAddressView).shouldHaveNoMoreInteractions()

        then(receiverNameView).should().visibility = View.GONE
        then(receiverNameView).shouldHaveNoMoreInteractions()
        then(receiverImageView).should().setAddress(TEST_RECEIVER)
        then(receiverImageView).shouldHaveNoMoreInteractions()
        then(receiverAddressView).should().text = "0x0029...0CBAA2"
        then(receiverAddressView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).should().loadAddressBookEntry(TEST_RECEIVER)
        then(addressBookRepository).should().loadAddressBookEntry(TEST_SAFE)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).should().loadSafe(TEST_RECEIVER)
        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testEtherToken() {
        val balancesObservable = TestObservableFactory<List<Pair<ERC20Token, BigInteger?>>>()
        setupViewMocks(balancesObservable.get())

        val data = TransactionData.AssetTransfer(ERC20Token.ETHER_TOKEN.address, TEST_AMOUNT, TEST_RECEIVER)
        viewHolder = AssetTransferViewHolder(TEST_SAFE, data, addressHelper, tokenRepositoryMock)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_asset_transfer_info, containerView, true)

        viewHolder.start()
        then(valueView).should().text = "~"
        then(valueView).should().text = "0.4 ETH"
        then(valueView).shouldHaveNoMoreInteractions()
        then(fiatView).should().text = "~ $"
        then(fiatView).shouldHaveNoMoreInteractions()

        then(safeBalanceView).shouldHaveZeroInteractions()
        balancesObservable.error(TimeoutException())
        then(safeBalanceView).should().text = null
        then(safeBalanceView).shouldHaveNoMoreInteractions()

        // Check view interaction
        assertAddressHelperInteractions()

        then(contextMock).shouldHaveZeroInteractions()

        // Check repository interaction
        then(tokenRepositoryMock).should().loadTokenBalances(TEST_SAFE, listOf(ERC20Token.ETHER_TOKEN))
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testKnownToken() {
        val tokenSingle = TestSingleFactory<ERC20Token>()
        val erc20Token = ERC20Token(TEST_TOKEN, "Test Token", "TT", 2, false)
        setupViewMocks(Observable.just(listOf(erc20Token to BigInteger.valueOf(1000))))
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(tokenSingle.get())

        val data = TransactionData.AssetTransfer(TEST_TOKEN, TEST_AMOUNT, TEST_RECEIVER)
        viewHolder = AssetTransferViewHolder(TEST_SAFE, data, addressHelper, tokenRepositoryMock)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_asset_transfer_info, containerView, true)

        viewHolder.start()
        then(valueView).should().text = "~"
        then(valueView).shouldHaveNoMoreInteractions()
        then(fiatView).should().text = "~ $"
        then(valueView).shouldHaveNoMoreInteractions()

        tokenSingle.success(erc20Token)
        then(valueView).should().text = "4000000000000000 TT"
        then(valueView).shouldHaveNoMoreInteractions()

        then(safeBalanceView).should().text = "10 TT"
        then(safeBalanceView).shouldHaveNoMoreInteractions()

        // Check view interaction
        assertAddressHelperInteractions()

        then(contextMock).shouldHaveZeroInteractions()

        // Check repository interaction
        then(tokenRepositoryMock).should().loadTokenBalances(TEST_SAFE, listOf(erc20Token))
        then(tokenRepositoryMock).should().loadToken(TEST_TOKEN)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testUnknownToken() {
        contextMock.mockGetString()
        given(viewHolderView.context).willReturn(contextMock)
        setupViewMocks(Observable.just(listOf()))
        val tokenInfoObservable = TestObservableFactory<ERC20Token>()
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(tokenRepositoryMock.loadTokenInfo(MockUtils.any())).willReturn(tokenInfoObservable.get())

        val data = TransactionData.AssetTransfer(TEST_TOKEN, TEST_AMOUNT, TEST_RECEIVER)
        viewHolder = AssetTransferViewHolder(TEST_SAFE, data, addressHelper, tokenRepositoryMock)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_asset_transfer_info, containerView, true)

        viewHolder.start()
        then(valueView).should().text = "~"
        then(valueView).shouldHaveNoMoreInteractions()
        then(fiatView).should().text = "~ $"
        then(fiatView).shouldHaveNoMoreInteractions()

        tokenInfoObservable.error(TimeoutException())

        then(fiatView).should().text = R.string.unknown_token.toString()
        then(fiatView).shouldHaveNoMoreInteractions()
        then(valueView).should().text = "400000000000000000"
        then(valueView).shouldHaveNoMoreInteractions()

        then(safeBalanceView).should().text = "0"
        then(safeBalanceView).shouldHaveNoMoreInteractions()

        // Check view interaction
        assertAddressHelperInteractions()

        then(contextMock).should().getString(R.string.unknown_token)
        then(contextMock).shouldHaveZeroInteractions()

        // Check repository interaction
        then(tokenRepositoryMock).should().loadTokenBalances(TEST_SAFE, listOf(ERC20Token(TEST_TOKEN, decimals = 0)))
        then(tokenRepositoryMock).should().loadToken(TEST_TOKEN)
        then(tokenRepositoryMock).should().loadTokenInfo(TEST_TOKEN)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testStop() {
        setupViewMocks(Observable.just(listOf()))
        val tokenSingle = TestSingleFactory<ERC20Token>()
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(tokenSingle.get())

        val data = TransactionData.AssetTransfer(TEST_TOKEN, TEST_AMOUNT, TEST_RECEIVER)
        viewHolder = AssetTransferViewHolder(TEST_SAFE, data, addressHelper, tokenRepositoryMock)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_asset_transfer_info, containerView, true)

        viewHolder.start()
        tokenSingle.assertAllSubscribed()

        viewHolder.stop()
        tokenSingle.assertAllCanceled()
    }

    @Test
    fun testDetach() {
        setupViewMocks(Observable.just(listOf()))
        val tokenSingle = TestSingleFactory<ERC20Token>()
        given(tokenRepositoryMock.loadToken(MockUtils.any())).willReturn(tokenSingle.get())

        val data = TransactionData.AssetTransfer(TEST_TOKEN, TEST_AMOUNT, TEST_RECEIVER)
        viewHolder = AssetTransferViewHolder(TEST_SAFE, data, addressHelper, tokenRepositoryMock)

        viewHolder.inflate(layoutInflater, containerView)
        then(layoutInflater).should().inflate(R.layout.layout_asset_transfer_info, containerView, true)

        viewHolder.start()
        tokenSingle.assertAllSubscribed()

        viewHolder.detach()
        tokenSingle.assertAllCanceled()

        then(addressBookRepository).should().loadAddressBookEntry(TEST_SAFE)
        then(addressBookRepository).should().loadAddressBookEntry(TEST_RECEIVER)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).should().loadSafe(TEST_SAFE)
        then(safeRepository).should().loadSafe(TEST_RECEIVER)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(tokenRepositoryMock).should().loadToken(TEST_TOKEN)
        then(tokenRepositoryMock).shouldHaveNoMoreInteractions()
        then(layoutInflater).shouldHaveNoMoreInteractions()

        // Check that it is detached from the view
        viewHolder.start()
        then(contextMock).shouldHaveZeroInteractions()
        then(addressBookRepository).shouldHaveZeroInteractions()
        then(safeRepository).shouldHaveZeroInteractions()
        then(tokenRepositoryMock).shouldHaveZeroInteractions()
        then(layoutInflater).shouldHaveZeroInteractions()
    }

    @Test
    fun testLoadTransaction() {
        val data = TransactionData.AssetTransfer(TEST_TOKEN, TEST_AMOUNT, TEST_RECEIVER)
        viewHolder = AssetTransferViewHolder(TEST_SAFE, data, addressHelper, tokenRepositoryMock)
        val testObserver = TestObserver<SafeTransaction>()
        viewHolder.loadTransaction().subscribe(testObserver)
        testObserver.assertResult(AssetTransferTransactionBuilder.build(data))
    }

    companion object {
        private val TEST_SAFE = "Fe2149773B3513703E79Ad23D05A778A185016ee".asEthereumAddress()!!
        private val TEST_TOKEN = "31b98d14007bDEE637298986988A0bbd31184523".asEthereumAddress()!!
        private val TEST_RECEIVER = "0029840843f03C164c29A78114b300Ba9E0CBAA2".asEthereumAddress()!!
        private val TEST_AMOUNT = BigInteger("400000000000000000")
    }

}
