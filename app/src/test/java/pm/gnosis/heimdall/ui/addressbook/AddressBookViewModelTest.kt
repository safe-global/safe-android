package pm.gnosis.heimdall.ui.addressbook

import android.arch.persistence.room.EmptyResultSetException
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Bitmap
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subscribers.TestSubscriber
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import pm.gnosis.tests.utils.mockGetString
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddressBookViewModelTest {
    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var addressBookRepositoryMock: AddressBookRepository

    @Mock
    private lateinit var safeRepositoryMock: GnosisSafeRepository

    @Mock
    private lateinit var qrCodeGeneratorMock: QrCodeGenerator

    private lateinit var viewModel: AddressBookViewModel

    @Before
    fun setUp() {
        viewModel = AddressBookViewModel(contextMock, addressBookRepositoryMock, safeRepositoryMock, qrCodeGeneratorMock)
        contextMock.mockGetString()
    }

    @Test
    fun testObserveAddressBook() {
        val testSubscriber = TestSubscriber<Adapter.Data<AddressBookEntry>>()
        val items = listOf(
            AddressBookEntry(TEST_ADDRESS, "A", ""),
            AddressBookEntry(TEST_ADDRESS, "B", ""),
            AddressBookEntry(TEST_ADDRESS, "C", "")
        )
        given(addressBookRepositoryMock.observeAddressBook()).willReturn(Flowable.just(items))

        viewModel.observeAddressBook().subscribe(testSubscriber)

        testSubscriber
            .assertValueAt(0, Adapter.Data())
            .assertValueAt(1) { it.entries == items }
            .assertNoErrors()
        then(addressBookRepositoryMock).should().observeAddressBook()
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testObserveAddressBookError() {
        val exception = Exception()
        val testSubscriber = TestSubscriber<Adapter.Data<AddressBookEntry>>()
        given(addressBookRepositoryMock.observeAddressBook()).willReturn(Flowable.error(exception))

        viewModel.observeAddressBook().subscribe(testSubscriber)

        testSubscriber.assertError(exception)
        then(addressBookRepositoryMock).should().observeAddressBook()
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
    }

    @Test
    fun testObserveAddressBookEntry() {
        val testSubscriber = TestSubscriber<AddressBookEntry>()
        val entry = AddressBookEntry(TEST_ADDRESS, "devops199", "")
        given(addressBookRepositoryMock.observeAddressBookEntry(MockUtils.any())).willReturn(Flowable.just(entry))

        viewModel.observeAddressBookEntry(TEST_ADDRESS).subscribe(testSubscriber)

        then(addressBookRepositoryMock).should().observeAddressBookEntry(TEST_ADDRESS)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        testSubscriber.assertValue(entry)
    }

    @Test
    fun testAddAddressBookEntry() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val testCompletable = TestCompletable()
        val name = "name"
        val description = "description"
        given(addressBookRepositoryMock.addAddressBookEntry(MockUtils.any(), anyString(), anyString())).willReturn(testCompletable)

        viewModel.addAddressBookEntry("0x1f81FFF89Bd57811983a35650296681f99C65C7E", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).should().addAddressBookEntry(TEST_ADDRESS, name, description)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver.assertValue(DataResult(AddressBookEntry(TEST_ADDRESS, name, description)))
    }

    @Test
    fun testAddAddressBookEntryInvalidAddress() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val testCompletable = TestCompletable()
        val name = "name"
        val description = "description"

        viewModel.addAddressBookEntry("test", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun testAddAddressBookEntryBlankName() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val testCompletable = TestCompletable()
        val name = "    "
        val description = "description"

        viewModel.addAddressBookEntry("0x0", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun testAddAddressBookEntryDuplicate() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val testCompletable = TestCompletable()
        val name = "name"
        val description = "description"
        given(addressBookRepositoryMock.addAddressBookEntry(MockUtils.any(), anyString(), anyString())).willReturn(
            Completable.error(
                SQLiteConstraintException()
            )
        )

        viewModel.addAddressBookEntry("0x1f81FFF89Bd57811983a35650296681f99C65C7E", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).should().addAddressBookEntry(TEST_ADDRESS, name, description)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun testAddAddressBookEntryOtherException() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val exception = Exception()
        val testCompletable = TestCompletable()
        val name = "name"
        val description = "description"
        given(addressBookRepositoryMock.addAddressBookEntry(MockUtils.any(), anyString(), anyString())).willReturn(Completable.error(exception))

        viewModel.addAddressBookEntry("0x1f81FFF89Bd57811983a35650296681f99C65C7E", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).should().addAddressBookEntry(TEST_ADDRESS, name, description)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue(ErrorResult(exception))
    }

    @Test
    fun testDeleteAddressBookEntry() {
        val testObserver = TestObserver<Result<Solidity.Address>>()
        val testCompletable = TestCompletable()
        given(addressBookRepositoryMock.deleteAddressBookEntry(MockUtils.any())).willReturn(testCompletable)
        given(safeRepositoryMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))

        viewModel.deleteAddressBookEntry(TEST_ADDRESS).subscribe(testObserver)

        then(addressBookRepositoryMock).should().deleteAddressBookEntry(TEST_ADDRESS)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        then(safeRepositoryMock).should().loadAbstractSafe(TEST_ADDRESS)
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver.assertValue(DataResult(TEST_ADDRESS))
    }

    @Test
    fun testDeleteAddressBookEntryIsSafe() {
        val testObserver = TestObserver<Result<Solidity.Address>>()
        given(safeRepositoryMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.just(Safe(TEST_ADDRESS)))
        val testCompletable = TestCompletable()
        given(addressBookRepositoryMock.deleteAddressBookEntry(MockUtils.any())).willReturn(testCompletable)

        viewModel.deleteAddressBookEntry(TEST_ADDRESS).subscribe(testObserver)

        then(safeRepositoryMock).should().loadAbstractSafe(TEST_ADDRESS)
        then(safeRepositoryMock).shouldHaveZeroInteractions()
        then(addressBookRepositoryMock).should().deleteAddressBookEntry(TEST_ADDRESS)
        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue(ErrorResult(SimpleLocalizedException(R.string.cannot_delete_safe_from_address_book.toString())))
    }

    @Test
    fun testDeleteAddressBookEntrySafeUnknownError() {
        val testObserver = TestObserver<Result<Solidity.Address>>()
        val exception = Exception()
        given(safeRepositoryMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(exception))
        val testCompletable = TestCompletable()
        given(addressBookRepositoryMock.deleteAddressBookEntry(MockUtils.any())).willReturn(testCompletable)

        viewModel.deleteAddressBookEntry(TEST_ADDRESS).subscribe(testObserver)

        then(safeRepositoryMock).should().loadAbstractSafe(TEST_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(addressBookRepositoryMock).should().deleteAddressBookEntry(TEST_ADDRESS)
        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue(ErrorResult(exception))
    }

    @Test
    fun testDeleteAddressBookEntryError() {
        val testObserver = TestObserver<Result<Solidity.Address>>()
        val exception = Exception()
        given(safeRepositoryMock.loadAbstractSafe(MockUtils.any())).willReturn(Single.error(EmptyResultSetException("")))
        given(addressBookRepositoryMock.deleteAddressBookEntry(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.deleteAddressBookEntry(TEST_ADDRESS).subscribe(testObserver)

        then(safeRepositoryMock).should().loadAbstractSafe(TEST_ADDRESS)
        then(safeRepositoryMock).shouldHaveNoMoreInteractions()
        then(addressBookRepositoryMock).should().deleteAddressBookEntry(TEST_ADDRESS)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception))
    }

    @Test
    fun testGenerateQrCode() {
        val testObserver = TestObserver<Result<Bitmap>>()
        val bitmap = mock(Bitmap::class.java)
        val color = 0
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.just(bitmap))

        viewModel.generateQrCode(TEST_ADDRESS, color).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(TEST_ADDRESS.asEthereumAddressString(), backgroundColor = color)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(bitmap))
    }

    @Test
    fun testGenerateQrCodeError() {
        val testObserver = TestObserver<Result<Bitmap>>()
        val exception = Exception()
        val color = 0
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.error(exception))

        viewModel.generateQrCode(TEST_ADDRESS, color).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(TEST_ADDRESS.asEthereumAddressString(), backgroundColor = color)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception))
    }

    @Test
    fun loadAddressBookEntry() {
        val testObserver = TestObserver.create<AddressBookEntry>()
        val address = Solidity.Address(0.toBigInteger())
        val addressBookEntry = AddressBookEntry(address, "", "")
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any())).willReturn(Single.just(addressBookEntry))

        viewModel.loadAddressBookEntry(address).subscribe(testObserver)

        then(addressBookRepositoryMock).should().loadAddressBookEntry(address)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertResult(addressBookEntry)
    }

    @Test
    fun loadAddressBookEntryError() {
        val testObserver = TestObserver.create<AddressBookEntry>()
        val address = Solidity.Address(0.toBigInteger())
        val exception = Exception()
        given(addressBookRepositoryMock.loadAddressBookEntry(MockUtils.any())).willReturn(Single.error(exception))

        viewModel.loadAddressBookEntry(address).subscribe(testObserver)

        then(addressBookRepositoryMock).should().loadAddressBookEntry(address)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertFailure(Exception::class.java)
    }

    @Test
    fun testUpdateAddressBookEntry() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val testCompletable = TestCompletable()
        val name = "name"
        given(addressBookRepositoryMock.updateAddressBookEntry(MockUtils.any(), anyString())).willReturn(testCompletable)

        viewModel.updateAddressBookEntry(TEST_ADDRESS, name).subscribe(testObserver)

        then(addressBookRepositoryMock).should().updateAddressBookEntry(TEST_ADDRESS, name)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver.assertValue(DataResult(AddressBookEntry(TEST_ADDRESS, name, "")))
    }

    @Test
    fun testUpdateAddressBookEntryBlankName() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val testCompletable = TestCompletable()
        val name = "   "
        viewModel.updateAddressBookEntry(TEST_ADDRESS, name).subscribe(testObserver)

        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue { it is ErrorResult && it.error is SimpleLocalizedException }
    }

    @Test
    fun testUpdateAddressBookEntryOtherException() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val exception = Exception()
        val testCompletable = TestCompletable()
        val name = "name"
        given(addressBookRepositoryMock.updateAddressBookEntry(MockUtils.any(), anyString())).willReturn(Completable.error(exception))

        viewModel.updateAddressBookEntry(TEST_ADDRESS, name).subscribe(testObserver)

        then(addressBookRepositoryMock).should().updateAddressBookEntry(TEST_ADDRESS, name)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue(ErrorResult(exception))
    }

    companion object {
        private val TEST_ADDRESS = "0x1f81FFF89Bd57811983a35650296681f99C65C7E".asEthereumAddress()!!
    }
}
