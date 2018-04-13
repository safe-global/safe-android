package pm.gnosis.heimdall.ui.addressbook

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
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.model.Solidity
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestCompletable
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.exceptions.InvalidAddressException
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddressBookViewModelTest {
    @Mock
    private lateinit var addressBookRepositoryMock: AddressBookRepository

    @Mock
    private lateinit var qrCodeGeneratorMock: QrCodeGenerator

    private lateinit var viewModel: AddressBookViewModel

    @Before
    fun setUp() {
        viewModel = AddressBookViewModel(addressBookRepositoryMock, qrCodeGeneratorMock)
    }

    @Test
    fun testObserveAddressBook() {
        val testSubscriber = TestSubscriber<Adapter.Data<AddressBookEntry>>()
        val items = listOf(
            AddressBookEntry(Solidity.Address(BigInteger.ZERO), "A", ""),
            AddressBookEntry(Solidity.Address(BigInteger.ZERO), "B", ""),
            AddressBookEntry(Solidity.Address(BigInteger.ZERO), "C", "")
        )
        given(addressBookRepositoryMock.observeAddressBook()).willReturn(Flowable.just(items))

        viewModel.observeAddressBook().subscribe(testSubscriber)

        testSubscriber
            .assertValueAt(0, Adapter.Data())
            .assertValueAt(1, { it.entries == items })
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
        val entry = AddressBookEntry(Solidity.Address(BigInteger.ZERO), "devops199", "")
        given(addressBookRepositoryMock.observeAddressBookEntry(MockUtils.any())).willReturn(Flowable.just(entry))

        viewModel.observeAddressBookEntry(Solidity.Address(BigInteger.ZERO)).subscribe(testSubscriber)

        then(addressBookRepositoryMock).should().observeAddressBookEntry(Solidity.Address(BigInteger.ZERO))
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

        viewModel.addAddressBookEntry("0x0", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).should().addAddressBookEntry(Solidity.Address(BigInteger.ZERO), name, description)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver.assertValue(DataResult(AddressBookEntry(Solidity.Address(BigInteger.ZERO), name, description)))
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
        testObserver.assertValue { it is ErrorResult && it.error is InvalidAddressException }
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
        testObserver.assertValue { it is ErrorResult && it.error is AddressBookContract.NameIsBlankException }
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

        viewModel.addAddressBookEntry("0x0", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).should().addAddressBookEntry(Solidity.Address(BigInteger.ZERO), name, description)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue { it is ErrorResult && it.error is AddressBookContract.AddressAlreadyAddedException }
    }

    @Test
    fun testAddAddressBookEntryOtherException() {
        val testObserver = TestObserver<Result<AddressBookEntry>>()
        val exception = Exception()
        val testCompletable = TestCompletable()
        val name = "name"
        val description = "description"
        given(addressBookRepositoryMock.addAddressBookEntry(MockUtils.any(), anyString(), anyString())).willReturn(Completable.error(exception))

        viewModel.addAddressBookEntry("0x0", name, description).subscribe(testObserver)

        then(addressBookRepositoryMock).should().addAddressBookEntry(Solidity.Address(BigInteger.ZERO), name, description)
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(0, testCompletable.callCount)
        testObserver.assertValue(ErrorResult(exception))
    }

    @Test
    fun testDeleteAddressBookEntry() {
        val testObserver = TestObserver<Result<Solidity.Address>>()
        val testCompletable = TestCompletable()
        given(addressBookRepositoryMock.deleteAddressBookEntry(MockUtils.any())).willReturn(testCompletable)

        viewModel.deleteAddressBookEntry(Solidity.Address(BigInteger.ZERO)).subscribe(testObserver)

        then(addressBookRepositoryMock).should().deleteAddressBookEntry(Solidity.Address(BigInteger.ZERO))
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        assertEquals(1, testCompletable.callCount)
        testObserver.assertValue(DataResult(Solidity.Address(BigInteger.ZERO)))
    }

    @Test
    fun testDeleteAddressBookEntryError() {
        val testObserver = TestObserver<Result<Solidity.Address>>()
        val exception = Exception()
        given(addressBookRepositoryMock.deleteAddressBookEntry(MockUtils.any())).willReturn(Completable.error(exception))

        viewModel.deleteAddressBookEntry(Solidity.Address(BigInteger.ZERO)).subscribe(testObserver)

        then(addressBookRepositoryMock).should().deleteAddressBookEntry(Solidity.Address(BigInteger.ZERO))
        then(addressBookRepositoryMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception))
    }

    @Test
    fun testGenerateQrCode() {
        val testObserver = TestObserver<Result<Bitmap>>()
        val bitmap = mock(Bitmap::class.java)
        val color = 0
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.just(bitmap))

        viewModel.generateQrCode(Solidity.Address(BigInteger.ZERO), color).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(Solidity.Address(BigInteger.ZERO).asEthereumAddressString(), backgroundColor = color)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(DataResult(bitmap))
    }

    @Test
    fun testGenerateQrCodeError() {
        val testObserver = TestObserver<Result<Bitmap>>()
        val exception = Exception()
        val color = 0
        given(qrCodeGeneratorMock.generateQrCode(anyString(), anyInt(), anyInt(), anyInt())).willReturn(Single.error(exception))

        viewModel.generateQrCode(Solidity.Address(BigInteger.ZERO), color).subscribe(testObserver)

        then(qrCodeGeneratorMock).should().generateQrCode(Solidity.Address(BigInteger.ZERO).asEthereumAddressString(), backgroundColor = color)
        then(qrCodeGeneratorMock).shouldHaveNoMoreInteractions()
        testObserver.assertValue(ErrorResult(exception))
    }
}
