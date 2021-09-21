package io.gnosis.safe.ui.settings.owner.ledger

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.data.utils.ExcludeClassFromJacocoGeneratedReport
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerOwnerSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.svalinn.common.utils.withArgs
import java.math.BigInteger

@ExcludeClassFromJacocoGeneratedReport
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
            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is DerivedOwners -> {

                        with(binding) {
                            showMoreOwners.setOnClickListener {
                                adapter.pagesVisible++
                                val visualFeedback = it.animate().alpha(0.0f)
                                visualFeedback.duration = 100
                                visualFeedback.setListener(object : Animator.AnimatorListener {

                                    override fun onAnimationRepeat(animation: Animator?) {}

                                    override fun onAnimationEnd(animation: Animator?) {
                                        adapter.notifyDataSetChanged()
                                        showMoreOwners.alpha = 1.0f
                                    }

                                    override fun onAnimationCancel(animation: Animator?) {}

                                    override fun onAnimationStart(animation: Animator?) {}
                                })
                                visualFeedback.start()
                                showMoreOwners.text = getString(R.string.signing_owner_selection_more)
                                showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                            }
                        }
                        lifecycleScope.launch {
                            if (derivationPath == viewAction.derivationPath) {
                                adapter.submitData(viewAction.newOwners)
                            }
                        }

                    }
                    is OwnerSelected -> {
                    }
                    else -> {
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
                    binding.progress.visible(false)
                    binding.showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                }
            }
        }

        with(binding) {
            owners.adapter = adapter
            owners.layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.loadOwners(derivationPath)

    }

    override fun onOwnerClicked(ownerIndex: Long, address: Solidity.Address) {
        val viewModel = (requireParentFragment() as LedgerTabsFragment).viewModel
        viewModel.setOwnerIndex(ownerIndex, address)
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

