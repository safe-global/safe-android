package io.gnosis.safe.ui.settings.chain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentChainSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class ChainSelectionFragment : BaseViewBindingFragment<FragmentChainSelectionBinding>() {

    private val navArgs by navArgs<ChainSelectionFragmentArgs>()
    private val mode by lazy { navArgs.mode }

    private lateinit var adapter: ChainSelectionAdapter

    @Inject
    lateinit var viewModel: ChainSelectionViewModel

    override fun screenId() = ScreenId.ASSETS_COINS

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentChainSelectionBinding =
        FragmentChainSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ChainSelectionAdapter()
        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            refresh.setOnRefreshListener {
                viewModel.loadChains()
            }
            chainList.adapter = adapter
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            dividerItemDecoration.setDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.divider)!!)
            chainList.addItemDecoration(dividerItemDecoration)
            chainList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (val action = state.viewAction) {
                is Loading -> {
                   showLoading()
                }
                is ShowChains -> {
                    binding.contentNoData.root.visible(false)
                    hideLoading()
                }
                is ShowError -> {
                    hideLoading()
                    if (adapter.itemCount == 0) {
                        binding.contentNoData.root.visible(true)
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadChains()
    }

    private fun showLoading() {
        binding.progress.visible(true)
        binding.refresh.isRefreshing = true
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
