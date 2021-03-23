package io.gnosis.safe.ui.settings.owner.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerListBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.showConfirmDialog
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.copyToClipboard
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class OwnerListFragment : BaseViewBindingFragment<FragmentOwnerListBinding>(), OwnerListAdapter.OwnerListener {

    @Inject
    lateinit var viewModel: OwnerListViewModel

    lateinit var adapter: OwnerListAdapter

    override fun screenId() = ScreenId.OWNER_LIST

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerListBinding =
        FragmentOwnerListBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = OwnerListAdapter(this)
        with(binding) {
            backButton.setOnClickListener {
                findNavController().navigateUp()
            }
            importButton.setOnClickListener {
                findNavController().navigate(OwnerListFragmentDirections.actionOwnerListFragmentToOwnerInfoFragment())
            }
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            dividerItemDecoration.setDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.divider)!!)
            owners.addItemDecoration(dividerItemDecoration)
            owners.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            owners.adapter = adapter
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is OwnerListState -> {
                    state.viewAction?.let { action ->
                        when (action) {
                            is Loading -> {
                                binding.progress.visible(true)
                            }
                            is LocalOwners -> {
                                binding.progress.visible(false)
                                if (action.owners.isEmpty()) {
                                    showEmptyState()
                                } else {
                                    adapter.updateData(action.owners)
                                    showList()
                                }
                            }
                            is ShowError -> {
                                binding.progress.visible(false)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadOwners()
    }

    override fun onOwnerRemove(owner: Solidity.Address, position: Int) {
        showConfirmDialog(requireContext(), R.string.signing_owner_dialog_description) {
            viewModel.removeOwner(owner)
            adapter.removeItem(position)
            if (adapter.itemCount == 0) {
                showEmptyState()
            }
            snackbar(requireView(), getString(R.string.signing_owner_key_removed))
        }
    }

    override fun onOwnerEdit(owner: Solidity.Address) {
        findNavController().navigate(OwnerListFragmentDirections.actionOwnerListFragmentToOwnerEditNameFragment(owner.asEthereumAddressString()))
    }

    override fun onOwnerClick(owner: Solidity.Address) {
        context?.copyToClipboard(getString(R.string.address_copied), owner.asEthereumAddressChecksumString()) {
            snackbar(view = binding.root, textId = R.string.copied_success)
        }
    }

    private fun showList() {
        with(binding) {
            owners.visible(true)
            emptyPlaceholder.visible(false)
        }
    }

    private fun showEmptyState() {
        with(binding) {
            owners.visible(false)
            emptyPlaceholder.visible(true)
        }
    }
}
