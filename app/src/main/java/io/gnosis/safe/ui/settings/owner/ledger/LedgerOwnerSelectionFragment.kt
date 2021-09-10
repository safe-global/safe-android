package io.gnosis.safe.ui.settings.owner.ledger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerOwnerSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.common.utils.withArgs
import timber.log.Timber
import java.math.BigInteger

class LedgerOwnerSelectionFragment : BaseViewBindingFragment<FragmentLedgerOwnerSelectionBinding>(),
    LedgerOwnerListAdapter.OnOwnerItemClickedListener {

    override fun screenId() = ScreenId.OWNER_SELECT_LEDGER_ACCOUNT

    override suspend fun chainId(): BigInteger? = null

    private lateinit var adapter: LedgerOwnerListAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLedgerOwnerSelectionBinding =
        FragmentLedgerOwnerSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val derivationPath = requireArguments()[ARGS_DERIVATION_PATH] as String
        val viewModel = (requireParentFragment() as LedgerTabsFragment).viewModel

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            Timber.i("LedgerOwnerSelectionFragment ----> state: $state")

            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is DerivedOwners -> {
                        Timber.i("LedgerOwnerSelectionFragment ----> DerivedOwners... ")

                        with(binding) {
                            showMoreOwners.setOnClickListener {
                                adapter.pagesVisible++
                                showMoreOwners.text = getString(R.string.signing_owner_selection_more)
                                showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                            }
                        }
                        lifecycleScope.launch {
                            adapter.submitData(viewAction.newOwners)
                        }

                    }
                    is EnableNextButton -> {
                        Timber.i("LedgerOwnerSelectionFragment ----> EnableNextButton... ")

                    }
                    else -> {
                        Timber.i("LedgerOwnerSelectionFragment ----> else... ")
                    }
                }
            }
        })

        adapter = LedgerOwnerListAdapter()
        adapter.setListener(this)
        adapter.addLoadStateListener { loadState ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                if (loadState.refresh is LoadState.Loading && adapter.itemCount == 0) {
                    binding.progress.visible(true)
                }

                if (viewModel.state.value?.viewAction is DerivedOwners && loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0) {
                    binding.showMoreOwners.visible(false)
                } else {

//                    viewModel.enableNextButton(adapter.itemCount > 0 && adapter.getSelectedOwnerIndex() == 0L && adapter.peek(0)?.disabled == false)

                    binding.progress.visible(false)
                    binding.showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                }
            }
        }

        with(binding) {
            owners.adapter = adapter
            owners.layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.loadFirstDerivedOwner(derivationPath)

    }

    override fun onResume() {
        super.onResume()
        Timber.i("LedgerOwnerSelectionFragment ----> onResume: this: $this, adapter: $adapter")
        Timber.i("LedgerOwnerSelectionFragment ----> onResume: this: $this, adapter: $adapter.")
    }

    override fun onOwnerClicked(ownerIndex: Long) {
        val viewModel = (requireParentFragment() as LedgerTabsFragment).viewModel
        viewModel.setOwnerIndex(ownerIndex)
    }

    companion object {
        private const val ARGS_DERIVATION_PATH = "args.string.derivation.path"
        private const val MAX_PAGES = LedgerOwnerPagingProvider.MAX_PAGES
        fun newInstance(derivationPath: String): LedgerOwnerSelectionFragment {
            return LedgerOwnerSelectionFragment().withArgs(Bundle().apply {
                putString(ARGS_DERIVATION_PATH, derivationPath)
            }) as LedgerOwnerSelectionFragment

        }
    }
}

