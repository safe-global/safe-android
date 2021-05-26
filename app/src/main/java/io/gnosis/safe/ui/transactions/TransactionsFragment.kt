package io.gnosis.safe.ui.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentTransactionsBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.safe.empty.NoSafeFragment
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class TransactionsFragment : SafeOverviewBaseFragment<FragmentTransactionsBinding>() {

    private val navArgs by navArgs<TransactionsFragmentArgs>()
    private val activeTab by lazy { navArgs.activeTab }

    @Inject
    lateinit var viewModel: TransactionsViewModel

    private lateinit var pager: TxPagerAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTransactionsBinding =
        FragmentTransactionsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            pager = TxPagerAdapter(this@TransactionsFragment)
            txContent.adapter = pager
            TabLayoutMediator(txTabBar, txContent, true) { tab, position ->
                when (TxPagerAdapter.Tabs.values()[position]) {
                    TxPagerAdapter.Tabs.QUEUE -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_tx_queue_24dp)
                        tab.text = getString(R.string.tx_tab_queue)
                    }
                    TxPagerAdapter.Tabs.HISTORY -> {
                        tab.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_tx_history_24dp)
                        tab.text = getString(R.string.tx_tab_history)
                    }
                }
            }.attach()
            txContent.setCurrentItem(activeTab, false)
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is TransactionsState -> {
                    val noActiveSafe = state.safe == null
                    pager.noActiveSafe = noActiveSafe
                    binding.txTabBar.visible(!noActiveSafe, View.INVISIBLE)
                    handleActiveSafe(state.safe)
                }
            }
        })
    }

    override fun handleActiveSafe(safe: Safe?) {
        navHandler?.setSafeData(safe)
    }

    override fun screenId(): ScreenId? = null
}

class TxPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { QUEUE, HISTORY }

    enum class Items(val value: Long) {
        QUEUE(Tabs.QUEUE.ordinal.toLong()),
        HISTORY(Tabs.HISTORY.ordinal.toLong()),
        NO_SAFE(RecyclerView.NO_ID)
    }

    var noActiveSafe: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = if (noActiveSafe) 1 else Tabs.values().size

    override fun createFragment(position: Int): Fragment {
        if (noActiveSafe) return NoSafeFragment.newInstance(NoSafeFragment.Position.TRANSACTIONS)
        return when (Tabs.values()[position]) {
            Tabs.QUEUE -> TransactionListFragment.newQueueInstance()
            Tabs.HISTORY -> TransactionListFragment.newHistoryInstance()
        }
    }

    override fun getItemId(position: Int): Long {
        return when (Tabs.values()[position]) {
            Tabs.QUEUE -> if (noActiveSafe) Items.NO_SAFE.value else Items.QUEUE.value
            Tabs.HISTORY -> Items.HISTORY.value
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when (itemId) {
            Items.NO_SAFE.value -> noActiveSafe
            Items.QUEUE.value -> !noActiveSafe
            else -> true
        }
    }
}
