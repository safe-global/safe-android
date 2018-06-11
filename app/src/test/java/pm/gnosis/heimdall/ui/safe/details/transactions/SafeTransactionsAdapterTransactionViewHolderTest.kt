package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import android.view.View
import android.widget.TextView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.tests.utils.mockFindViewById
import pm.gnosis.tests.utils.mockGetString

@RunWith(MockitoJUnitRunner::class)
class SafeTransactionsAdapterTransactionViewHolderTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var itemView: View

    @Mock
    private lateinit var headerTextView: TextView

    lateinit var viewHolder: SafeTransactionsAdapter.HeaderViewHolder

    @Before
    fun setUp() {
        viewHolder = SafeTransactionsAdapter.HeaderViewHolder(itemView)
    }

    @Test
    fun bindHeaderEntry() {
        context.mockGetString()
        given(itemView.context).willReturn(context)
        itemView.mockFindViewById(R.id.layout_adapter_entry_header_title, headerTextView)

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Header(R.string.header_submitted), emptyList())

        then(headerTextView).should().text = R.string.header_submitted.toString()
        then(headerTextView).shouldHaveNoMoreInteractions()
    }

    @Test
    fun bindOtherEntry() {
        //Nothing should happen
        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("some_id"), emptyList())

        then(itemView).shouldHaveZeroInteractions()
    }
}
