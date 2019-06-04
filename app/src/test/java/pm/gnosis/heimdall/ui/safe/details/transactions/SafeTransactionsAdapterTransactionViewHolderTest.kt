package pm.gnosis.heimdall.ui.safe.details.transactions

import android.content.Context
import android.view.View
import android.widget.TextView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.ui.safe.details.transactions.test.SUBMITTED
import pm.gnosis.tests.utils.mockFindViewById

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
        itemView.mockFindViewById(R.id.layout_adapter_entry_header_title, headerTextView)

        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Header(SUBMITTED), emptyList())

        then(headerTextView).should().text = SUBMITTED
        then(headerTextView).shouldHaveNoMoreInteractions()
    }

    @Test
    fun bindOtherEntry() {
        //Nothing should happen
        viewHolder.bind(SafeTransactionsContract.AdapterEntry.Transaction("some_id"), emptyList())

        then(itemView).shouldHaveZeroInteractions()
    }
}
