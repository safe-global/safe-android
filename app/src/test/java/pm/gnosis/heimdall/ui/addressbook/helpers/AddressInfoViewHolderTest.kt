package pm.gnosis.heimdall.ui.addressbook.helpers

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.view.View
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.blockies.BlockiesImageView
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.common.utils.visible
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.models.AddressBookEntry
import pm.gnosis.tests.utils.ImmediateSchedulersRule
import pm.gnosis.tests.utils.MockUtils
import pm.gnosis.tests.utils.TestFlowableFactory
import pm.gnosis.tests.utils.mockFindViewById
import pm.gnosis.utils.asEthereumAddressString
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class AddressInfoViewHolderTest {
    @JvmField
    @Rule
    val rule = ImmediateSchedulersRule()

    @Mock
    private lateinit var contextMock: Context

    @Mock
    private lateinit var addressBookRepositoryMock: AddressBookRepository

    @Mock
    private lateinit var lifecycleMock: Lifecycle

    @Mock
    private lateinit var lifecycleOwnerMock: LifecycleOwner

    @Mock
    private lateinit var viewFactoryMock: ViewFactory

    @Mock
    private lateinit var viewMock: View

    @Mock
    private lateinit var iconViewMock: BlockiesImageView

    @Mock
    private lateinit var nameViewMock: TextView

    @Mock
    private lateinit var valueViewMock: TextView

    private lateinit var application: HeimdallApplication

    private lateinit var viewHolder: AddressInfoViewHolder

    private fun View.setupMock(): View {
        given(context).willReturn(contextMock)
        return this
    }

    @Before
    fun setUp() {
        // Setup injection mocks (super hacky for now, we should review that later)
        application = HeimdallApplication()
        given(application.component.addressBookRepository()).willReturn(addressBookRepositoryMock)
        given(contextMock.applicationContext).willReturn(application)

        // Setup view mocks
        viewMock.setupMock()
        viewMock.mockFindViewById(R.id.layout_address_item_icon, iconViewMock)
        viewMock.mockFindViewById(R.id.layout_address_item_value, valueViewMock)
        viewMock.mockFindViewById(R.id.layout_address_item_name, nameViewMock)
        given(lifecycleOwnerMock.lifecycle).willReturn(lifecycleMock)
        given(viewFactoryMock.get()).willReturn(viewMock)
        viewHolder = AddressInfoViewHolder(lifecycleOwnerMock, viewFactoryMock)
    }

    @Test
    fun notAttached() {
        val flowableFactory = TestFlowableFactory<AddressBookEntry>()
        given(addressBookRepositoryMock.observeAddressBookEntry(MockUtils.any())).willReturn(flowableFactory.get())

        then(viewMock).should().addOnAttachStateChangeListener(viewHolder)
        then(viewMock).should().tag = viewHolder
        viewHolder.bind(BigInteger.TEN)

        assertEquals("Correct address should be set", BigInteger.TEN, viewHolder.currentAddress)

        then(iconViewMock).should().setAddress(BigInteger.TEN)
        then(valueViewMock).should().text = BigInteger.TEN.asEthereumAddressString()
        then(valueViewMock).should().visible(true)
        then(nameViewMock).should().visible(false)
        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        then(lifecycleMock).shouldHaveZeroInteractions()

        viewHolder.onViewAttachedToWindow(viewMock)

        then(lifecycleMock).should().addObserver(viewHolder)
        then(lifecycleMock).shouldHaveNoMoreInteractions()
        then(addressBookRepositoryMock).shouldHaveZeroInteractions()

        // Simulate lifecycle start
        viewHolder.start()

        then(addressBookRepositoryMock).should().observeAddressBookEntry(BigInteger.TEN)
        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        flowableFactory.assertCount(1).assertAllSubscribed()

        flowableFactory.offer(AddressBookEntry(BigInteger.TEN, "Test Name", ""))

        then(nameViewMock).should().visible(true)
        then(nameViewMock).should().text = "Test Name"

        // Simulate lifecycle end
        viewHolder.stop()

        flowableFactory.assertCount(1).assertAllCanceled()

        viewHolder.onViewDetachedFromWindow(viewMock)

        then(lifecycleMock).should().removeObserver(viewHolder)
        then(lifecycleMock).shouldHaveNoMoreInteractions()

        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
        then(iconViewMock).shouldHaveZeroInteractions()
        then(valueViewMock).shouldHaveZeroInteractions()
        then(nameViewMock).shouldHaveZeroInteractions()
    }

    @Test
    fun attached() {
        val flowableFactory = TestFlowableFactory<AddressBookEntry>()
        given(addressBookRepositoryMock.observeAddressBookEntry(MockUtils.any())).willReturn(flowableFactory.get())

        then(viewMock).should().addOnAttachStateChangeListener(viewHolder)
        then(viewMock).should().tag = viewHolder

        given(viewMock.isAttachedToWindow).willReturn(true)

        viewHolder.bind(BigInteger.TEN)

        assertEquals("Correct address should be set", BigInteger.TEN, viewHolder.currentAddress)

        then(iconViewMock).should().setAddress(BigInteger.TEN)
        then(valueViewMock).should().text = BigInteger.TEN.asEthereumAddressString()
        then(valueViewMock).should().visible(true)
        then(nameViewMock).should().visible(false)
        then(lifecycleMock).should().addObserver(viewHolder)
        then(lifecycleMock).shouldHaveNoMoreInteractions()

        viewHolder.start()
        then(addressBookRepositoryMock).should().observeAddressBookEntry(BigInteger.TEN)
        then(addressBookRepositoryMock).shouldHaveZeroInteractions()

        flowableFactory.assertCount(1).assertAllSubscribed()

        viewHolder.onViewDetachedFromWindow(viewMock)
        // Detach from window should call stop()
        flowableFactory.assertCount(1).assertAllCanceled()

        then(lifecycleMock).should().removeObserver(viewHolder)
        then(lifecycleMock).shouldHaveNoMoreInteractions()
        then(addressBookRepositoryMock).shouldHaveZeroInteractions()
    }
}