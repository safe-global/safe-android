package pm.gnosis.heimdall.helpers

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
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddressHelperTest {

    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    lateinit var addressBookRepository: AddressBookRepository

    @Mock
    lateinit var safeRepository: GnosisSafeRepository

    @Mock
    lateinit var addressView: TextView

    @Mock
    lateinit var nameView: TextView

    @Mock
    lateinit var imageView: BlockiesImageView

    private lateinit var helper: AddressHelper

    @Before
    fun setUp() {
        helper = AddressHelper(addressBookRepository, safeRepository)
    }

    @Test
    fun testUnknownAddress() {
        val testAddress = Solidity.Address(BigInteger.TEN)
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(safeRepository.loadSafe(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))

        helper.populateAddressInfo(addressView, nameView, null, testAddress)

        then(imageView).shouldHaveNoMoreInteractions()
        then(addressView).should().text = "0x0000...00000A"
        then(addressView).shouldHaveNoMoreInteractions()
        then(nameView).shouldHaveZeroInteractions()

        then(safeRepository).should().loadSafe(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
        then(addressBookRepository).should().loadAddressBookEntry(testAddress)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
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
        then(addressView).should().text = "0xA7e1...a246EC"
        then(addressView).shouldHaveNoMoreInteractions()
        then(nameView).should().text = "I need more Mate!"
        then(nameView).should().visibility = View.VISIBLE
        then(nameView).shouldHaveNoMoreInteractions()

        then(safeRepository).shouldHaveZeroInteractions()
        then(addressBookRepository).should().loadAddressBookEntry(testAddress)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testSafeAddress() {
        val testAddress = "0x31B98D14007bDEe637298086988A0bBd31184523".asEthereumAddress()!!
        val safe = Safe(testAddress, "The mate vault!")
        given(addressBookRepository.loadAddressBookEntry(MockUtils.any())).willReturn(Single.error(NoSuchElementException()))
        given(safeRepository.loadSafe(MockUtils.any()))
            .willReturn(Single.just(safe))

        helper.populateAddressInfo(addressView, nameView, imageView, testAddress)

        then(imageView).should().setAddress(testAddress)
        then(imageView).shouldHaveNoMoreInteractions()
        then(addressView).should().text = "0x31B9...184523"
        then(addressView).shouldHaveNoMoreInteractions()
        then(nameView).should().text = "The mate vault!"
        then(nameView).should().visibility = View.VISIBLE
        then(nameView).shouldHaveNoMoreInteractions()

        then(addressBookRepository).should().loadAddressBookEntry(testAddress)
        then(addressBookRepository).shouldHaveNoMoreInteractions()
        then(safeRepository).should().loadSafe(testAddress)
        then(safeRepository).shouldHaveNoMoreInteractions()
    }
}
