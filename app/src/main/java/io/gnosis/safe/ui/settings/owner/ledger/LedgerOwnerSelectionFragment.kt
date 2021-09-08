package io.gnosis.safe.ui.settings.owner.ledger

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerOwnerSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.owner.intro.OwnerInfoLedgerFragmentDirections
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import java.math.BigInteger
import javax.inject.Inject

class LedgerOwnerSelectionFragment : BaseViewBindingFragment<FragmentLedgerOwnerSelectionBinding>(),
    LedgerOwnerListAdapter.OnOwnerItemClickedListener {

    override fun screenId() = ScreenId.OWNER_SELECT_LEDGER_ACCOUNT

    override suspend fun chainId(): BigInteger? = null

    @Inject
    lateinit var viewModel: LedgerOwnerSelectionViewModel

    private lateinit var adapter: LedgerOwnerListAdapter

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLedgerOwnerSelectionBinding =
        FragmentLedgerOwnerSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

                    binding.nextButton.isEnabled =
                        adapter.itemCount > 0 && adapter.getSelectedOwnerIndex() == 0L && adapter.peek(0)?.disabled == false
                    binding.progress.visible(false)
                    binding.showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                }
            }
        }

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            nextButton.setOnClickListener {
                findNavController().navigate(
                    //TODO add navigation to Enter name fragment
                    OwnerInfoLedgerFragmentDirections.actionOwnerInfoLedgerFragmentToLedgerOwnerSelectionFragment()
                )
            }
            owners.adapter = adapter
            owners.layoutManager = LinearLayoutManager(requireContext())
        }

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
                            adapter.submitData(viewAction.newOwners)
                        }

                    }
                    else -> {
                    }
                }
            }
        })

        viewModel.loadFirstDerivedOwner("m/44'/60'/0'/0/{index}") // ledger live is the default derivation path

    }

    override fun onOwnerClicked(ownerIndex: Long) {
        binding.nextButton.isEnabled = true
        viewModel.setOwnerIndex(ownerIndex)
    }

    companion object {
        private const val MAX_PAGES = LedgerOwnerPagingProvider.MAX_PAGES
    }
}
