package io.gnosis.safe.ui.settings.chain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.data.models.Chain
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentChainSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import kotlinx.coroutines.launch
import pm.gnosis.svalinn.common.utils.visible
import java.math.BigInteger
import javax.inject.Inject

class ChainSelectionFragment : BaseViewBindingFragment<FragmentChainSelectionBinding>() {

    override fun screenId() = ScreenId.CHAIN_LIST

    override suspend fun chainId(): BigInteger? = null

    private val navArgs by navArgs<ChainSelectionFragmentArgs>()
    private val mode by lazy { navArgs.mode }

    private val adapter by lazy { ChainSelectionAdapter(mode) }

    @Inject
    lateinit var viewModel: ChainSelectionViewModel


    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentChainSelectionBinding =
        FragmentChainSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter.addLoadStateListener { loadState ->

            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                binding.progress.isVisible = loadState.refresh is LoadState.Loading && adapter.itemCount == 0
                binding.refresh.isRefreshing = loadState.refresh is LoadState.Loading && adapter.itemCount != 0

                loadState.refresh.let {
                    when (it) {
                        is LoadState.Error -> {
                            if (adapter.itemCount == 0) {
                                binding.selectChainTitle.visible(false)
                                binding.contentNoData.root.visible(true)
                            }
                            handleError(it.error)
                        }
                        is LoadState.Loading -> {
                            binding.contentNoData.root.visible(false)
                        }
                        is LoadState.NotLoading -> {
                            if (viewModel.state.value?.viewAction is ShowChains && adapter.itemCount == 0) {
                                binding.selectChainTitle.visible(false)
                                binding.contentNoData.root.visible(true)
                            } else {
                                binding.selectChainTitle.visible(mode == ChainSelectionMode.ADD_SAFE)
                                binding.contentNoData.root.visible(false)
                            }
                        }
                    }
                }
                loadState.append.let {
                    if (it is LoadState.Error) handleError(it.error)
                }
                loadState.prepend.let {
                    if (it is LoadState.Error) handleError(it.error)
                }
            }
        }

        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            refresh.setOnRefreshListener {
                viewModel.loadChains(true)
            }
            chainList.adapter = adapter
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            dividerItemDecoration.setDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.divider)!!)
            chainList.addItemDecoration(dividerItemDecoration)
            chainList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val action = state.viewAction) {
                is ShowChains -> {
                    loadChains(action.chains)
                }
                is ShowError -> {
                    hideLoading()
                    if (adapter.itemCount == 0) {
                        binding.selectChainTitle.visible(false)
                        binding.contentNoData.root.visible(true)
                    }
                    handleError(action.error)
                }
            }
        })

        viewModel.loadChains()
    }

    private fun loadChains(newChains: PagingData<Chain>) {
        lifecycleScope.launch {
            adapter.submitData(newChains)
        }
    }

    private fun handleError(throwable: Throwable) {
        val error = throwable.toError()
        if (error.trackingRequired) {
            tracker.logException(throwable)
        }
        errorSnackbar(requireView(), error.message(requireContext(), R.string.error_description_chain_list))
    }

    private fun hideLoading() {
        binding.progress.visible(false)
        binding.refresh.isRefreshing = false
    }
}

enum class ChainSelectionMode {
    ADD_SAFE,
    CHAIN_DETAILS
}
