package io.gnosis.safe.ui.assets.collectibles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentCollectiblesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.assets.collectibles.paging.CollectibleLoadStateAdapter
import io.gnosis.safe.ui.assets.collectibles.paging.CollectibleViewDataListAdapter
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class CollectiblesFragment : BaseViewBindingFragment<FragmentCollectiblesBinding>() {

    @Inject
    lateinit var viewModel: CollectiblesViewModel

    private val adapter by lazy { CollectibleViewDataListAdapter(CollectiblesViewHolderFactory()) }

    override fun screenId() = ScreenId.ASSETS_COLLECTIBLES

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCollectiblesBinding =
        FragmentCollectiblesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.addLoadStateListener { loadState ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                binding.progress.isVisible =
                    loadState.refresh is LoadState.Loading && adapter.itemCount == 0
                binding.refresh.isRefreshing =
                    loadState.refresh is LoadState.Loading && adapter.itemCount != 0

                loadState.refresh.let {
                    if (it is LoadState.Error) {
                        if (adapter.itemCount == 0) {
                            binding.contentNoData.root.visible(true)
                        }
                        handleError(it.error)
                    }
                }
                loadState.append.let {
                    if (it is LoadState.Error) handleError(it.error)
                }
                loadState.prepend.let {
                    if (it is LoadState.Error) handleError(it.error)
                }

                if (viewModel.state.value?.viewAction is UpdateCollectibles && loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0) {
                    showEmptyState()
                } else {
                    showList()
                }
            }
        }

        with(binding.collectibles) {
            adapter = this@CollectiblesFragment.adapter.withLoadStateHeaderAndFooter(
                header = CollectibleLoadStateAdapter { this@CollectiblesFragment.adapter.retry() },
                footer = CollectibleLoadStateAdapter { this@CollectiblesFragment.adapter.retry() }
            )
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.refresh.setOnRefreshListener { viewModel.load(true) }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

            binding.emptyPlaceholder.visible(false)
            binding.contentNoData.root.visible(false)
            binding.progress.visible(state.loading)
            binding.refresh.isRefreshing = state.refreshing

            state.viewAction?.let { action ->
                when (action) {
                    is UpdateActiveSafe -> {
                        lifecycleScope.launch {
                            // if safe changes we need to reset data for recycler
                            adapter.submitData(PagingData.empty())
                        }
                    }
                    is ShowEmptyState -> {
                        showEmptyState()
                    }
                    is UpdateCollectibles -> {
                        binding.contentNoData.root.visibility = View.GONE
                        lifecycleScope.launch {
                            adapter.submitData(action.collectibles)
                        }
                    }
                    is ShowError -> {
                        hideLoading()
                        if (adapter.itemCount == 0) {
                            binding.contentNoData.root.visible(true)
                        }
                        handleError(action.error)
                    }
                    else -> {

                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isLoading()) {
            viewModel.load(true)
        }
    }

    private fun hideLoading() {
        binding.progress.visible(false)
        binding.refresh.isRefreshing = false
    }

    private fun handleError(throwable: Throwable) {
        val error = throwable.toError()
        if (error.trackingRequired) {
            tracker.logException(throwable)
        }
        errorSnackbar(
            requireView(),
            error.message(requireContext(), R.string.error_description_assets_collectibles)
        )
    }

    private fun showList() {
        with(binding) {
            collectibles.visible(true)
            contentNoData.root.visible(false)
        }
    }

    private fun showEmptyState() {
        with(binding) {
            collectibles.visible(false)
            emptyPlaceholder.visible(true)
        }
    }

    companion object {
        fun newInstance(): CollectiblesFragment = CollectiblesFragment()
    }
}
