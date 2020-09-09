package io.gnosis.safe.ui.assets.collectibles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentCollectiblesBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.*
import io.gnosis.safe.ui.base.adapter.Adapter
import io.gnosis.safe.ui.base.adapter.MultiViewHolderAdapter
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.getErrorResForException
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class CollectiblesFragment : BaseViewBindingFragment<FragmentCollectiblesBinding>() {

    @Inject
    lateinit var viewModel: CollectiblesViewModel

    private val adapter by lazy { MultiViewHolderAdapter(CollectiblesViewHolderFactory()) }

    override fun screenId() = ScreenId.ASSETS_COLLECTIBLES

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCollectiblesBinding =
        FragmentCollectiblesBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            collectibles.adapter = adapter
            collectibles.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            refresh.setOnRefreshListener { viewModel.load(true) }
        }
        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

            binding.emptyPlaceholder.visible(false)
            binding.contentNoData.root.visible(false)
            binding.progress.visible(state.loading)
            binding.refresh.isRefreshing = state.refreshing

            state.viewAction?.let { action ->
                when (action) {
                    is UpdateActiveSafe -> {
                        adapter.updateData(Adapter.Data(null, listOf()))
                    }
                    is ShowEmptyState -> {
                        binding.emptyPlaceholder.visible(true)
                    }
                    is UpdateCollectibles -> {
                        binding.contentNoData.root.visibility = View.GONE
                        adapter.updateData(action.collectibles)
                    }
                    is ShowError -> {
                        hideLoading()
                        if (adapter.itemCount == 0) {
                            binding.contentNoData.root.visible(true)
                        }
                        when (action.error) {
                            is Offline -> {
                                snackbar(requireView(), R.string.error_no_internet)
                            }
                            else -> {
                                snackbar(requireView(), action.error.getErrorResForException())
                            }
                        }
                    }
                    else -> {

                    }
                }
            }
        })
    }

    private fun hideLoading() {
        binding.progress.visible(false)
        binding.refresh.isRefreshing = false
    }

    companion object {
        fun newInstance(): CollectiblesFragment = CollectiblesFragment()
    }
}
