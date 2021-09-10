package io.gnosis.safe.ui.settings.owner.ledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.intro.OwnerInfoLedgerFragmentDirections
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

class LedgerTabsFragment : BaseViewBindingFragment<FragmentLedgerBinding>() {

    override fun screenId() = ScreenId.OWNER_SELECT_LEDGER_ACCOUNT

    override suspend fun chainId(): BigInteger? = null

    private lateinit var pager: LedgerPagerAdapter

    @Inject
    @Singleton
    lateinit var viewModel: LedgerOwnerSelectionViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLedgerBinding =
        FragmentLedgerBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            Timber.i("LedgerTabsFragment ----> state: $state")
            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is DerivedOwners -> {
                        // ignore here
                        Timber.i("LedgerTabsFragment ----> DerivedOwners... ")
                    }
                    is EnableNextButton -> {
                        Timber.i("LedgerTabsFragment ----> EnableNextButton... ")
                        binding.nextButton.isEnabled = true
                    }
                }
            }
        })

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            nextButton.setOnClickListener {
                Timber.i("LedgerTabsFragment ----> Goto next screen... ")
                findNavController().navigate(OwnerInfoLedgerFragmentDirections.actionOwnerInfoLedgerFragmentToLedgerTabsFragment())
            }
            pager = LedgerPagerAdapter(this@LedgerTabsFragment)
            ledgerContent.adapter = pager
            TabLayoutMediator(ledgerTabBar, ledgerContent, true) { tab, position ->
                when (LedgerPagerAdapter.Tabs.values()[position]) {
                    LedgerPagerAdapter.Tabs.LEDGER_LIVE -> {
                        tab.text = getString(R.string.ledger_tab_ledger_live)
                    }
                    LedgerPagerAdapter.Tabs.LEDGER -> {
                        tab.text = getString(R.string.ledger_tab_ledger)
                    }
                }
            }.attach()
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.i("LedgerTabsFragment ----> onResume: this: $this")
    }
}

class LedgerPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    enum class Tabs { LEDGER_LIVE, LEDGER }

    enum class Items(val value: Long) {
        LEDGER_LIVE(Tabs.LEDGER_LIVE.ordinal.toLong()),
        LEDGER(Tabs.LEDGER.ordinal.toLong()),
        NO_SAFE(RecyclerView.NO_ID)
    }

    var noActiveSafe: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.LEDGER_LIVE -> {
                LedgerOwnerSelectionFragment.newInstance("m/44'/60'/0'/0/{index}")
            }
            Tabs.LEDGER -> {
                LedgerOwnerSelectionFragment.newInstance("")
//                LedgerOwnerSelectionFragment.newInstance("m/44'/60'/0'/{index}")
            }
        }

    override fun getItemId(position: Int): Long {
        return when (Tabs.values()[position]) {
            Tabs.LEDGER_LIVE -> Items.LEDGER_LIVE.value
            Tabs.LEDGER -> Items.LEDGER.value
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return when (itemId) {
            Items.NO_SAFE.value -> noActiveSafe
            Items.LEDGER_LIVE.value -> !noActiveSafe
            else -> true
        }
    }
}
