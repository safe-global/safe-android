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
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentLedgerOwnerSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.launch
import pm.gnosis.model.Solidity
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

            Timber.i("----> loadState: $loadState")

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                if (loadState.refresh is LoadState.Loading && adapter.itemCount == 0) {
                    Timber.i("----> Show progress")

                    binding.progress.visible(true)
                    binding.emptyPlaceholder.visible(false)
//                    binding.
                }

                if (viewModel.state.value?.viewAction is DerivedOwners && loadState.refresh is LoadState.NotLoading && adapter.itemCount > 0) {
                    Timber.i("----> showMoreOwners.visible(${adapter.pagesVisible < MAX_PAGES})")
                    binding.showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                }

                if (loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0) {
                    Timber.i("----> showEmptyState()")
                    showEmptyState()
                }
                if (adapter.itemCount > 0) {
                    Timber.i("----> showList()")

                    showList()
                }
            }
            loadState.append.let {
                if (it is LoadState.Error) {
                    Timber.i("----> Handle Error (append) ${it.error}")

                    handleError(it.error)
                    showEmptyState()
                }
            }
            loadState.prepend.let {
                if (it is LoadState.Error) {
                    Timber.i("----> Handle Error (prepend) ${it.error}")

                    handleError(it.error)
                    showEmptyState()
                }
            }
            loadState.refresh.let {
                if (it is LoadState.Error) {
                    Timber.i("----> Handle Error (refresh) ${it.error}")
                    handleError(it.error)
                    showEmptyState()
                }
            }

        }

        with(binding) {
            owners.adapter = adapter
            owners.layoutManager = LinearLayoutManager(requireContext())
        }

        viewModel.loadOwners(derivationPath)

    }

    private fun handleError(throwable: Throwable) {
        val error = throwable.toError()
        if (error.trackingRequired) {
            tracker.logException(throwable)
        }
        errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_ledger_address_list))
    }

    private fun showList() {
//        startElapsedIntervalsUpdate()
        with(binding) {
            progress.visible(false)
            derivedOwners.visible(true)
            emptyPlaceholder.visible(false)
        }
    }

    private fun showEmptyState() {
//        stopElapsedIntervalsUpdate()
        with(binding) {
            derivedOwners.visible(false)
            emptyPlaceholder.visible(true)
            binding.progress.visible(false)
        }
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

