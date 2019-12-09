package pm.gnosis.heimdall.helpers

import android.content.Context
import android.view.View
import android.widget.TextView
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.utils.asMiddleEllipsized
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddressHelperTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var contextMock: Context

    @Mock
    lateinit var addressBookRepository: AddressBookRepository

    @Mock
    lateinit var addressView: TextView

    @Mock
    lateinit var nameView: TextView

    @Mock
    lateinit var imageView: BlockiesImageView

    private lateinit var helper: AddressHelper

    @Before
    fun setUp() {
        helper = AddressHelper(addressBookRepository)
    }

    @Test
    fun testUnknownAddress() {
        val testAddress = Solidity.Address(BigInteger.TEN)
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))

        helper.populateAddressInfo(addressView, nameView, null, testAddress)

        then(imageView).shouldHaveNoMoreInteractions()
        then(addressView).should().text = "0x00...000A"
        then(addressView).should().setOnClickListener(MockUtils.any())
        then(addressView).shouldHaveNoMoreInteractions()
        then(nameView).shouldHaveZeroInteractions()

        then(addressBookRepository).should().loadAddressBookEntry(testAddress)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testMultiSendAddress() {
        given(addressView.context).willReturn(contextMock)
        contextMock.mockGetString()
        val testAddress = BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!!

        helper.populateAddressInfo(addressView, nameView, null, testAddress)

        then(imageView).shouldHaveNoMoreInteractions()
        then(addressView).should().text = testAddress.asEthereumAddressChecksumString().asMiddleEllipsized(4)
        then(addressView).should().setOnClickListener(MockUtils.any())
        then(addressView).should().context
        then(addressView).shouldHaveNoMoreInteractions()
        then(nameView).should().text = "${R.string.multi_send_contract}"
        then(nameView).should().visibility = View.VISIBLE
        then(nameView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun testMultiSendOldAddress() {
        given(addressView.context).willReturn(contextMock)
        contextMock.mockGetString()
        val testAddress = BuildConfig.MULTI_SEND_OLD_ADDRESS.asEthereumAddress()!!

        helper.populateAddressInfo(addressView, nameView, null, testAddress)

        then(imageView).shouldHaveNoMoreInteractions()
        then(addressView).should().text = testAddress.asEthereumAddressChecksumString().asMiddleEllipsized(4)
        then(addressView).should().setOnClickListener(MockUtils.any())
        then(addressView).should().context
        then(addressView).shouldHaveNoMoreInteractions()
        then(nameView).should().text = "${R.string.multi_send_contract}"
        then(nameView).should().visibility = View.VISIBLE
        then(nameView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).shouldHaveZeroInteractions()
    }

    @Test
    fun testAddressBookAddress() {
        val testAddress = "0xA7e15e2e76Ab469F8681b576cFF168F37Aa246EC".asEthereumAddress()!!
        val addressBookEntry = AddressBookEntry(testAddress, "I need more Mate!", "Mate has sugar! Sugar = Energy! That gets me going!")
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any()))
            .willReturn(Single.just(addressBookEntry))

        helper.populateAddressInfo(addressView, nameView, imageView, testAddress)

        then(imageView).should().setAddress(testAddress)
        then(imageView).shouldHaveNoMoreInteractions()
        then(addressView).should().text = "0xA7...46EC"
        then(addressView).should().setOnClickListener(MockUtils.any())
        then(addressView).shouldHaveNoMoreInteractions()
        then(nameView).should().text = "I need more Mate!"
        then(nameView).should().visibility = View.VISIBLE
        then(nameView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).should().loadAddressBookEntry(testAddress)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
    }
}
