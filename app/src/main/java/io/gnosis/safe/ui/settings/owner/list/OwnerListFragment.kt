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
import io.gnosis.data.models.Owner
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerListBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.Loading
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.ShowError
import io.gnosis.safe.ui.base.SafeOverviewBaseFragment
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.utils.getFromCurrent
import io.gnosis.safe.utils.setToCurrent
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.snackbar
import pm.gnosis.svalinn.common.utils.visible
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class OwnerListFragment : BaseViewBindingFragment<FragmentOwnerListBinding>(), OwnerListAdapter.OwnerListener {

    @Inject
    lateinit var viewModel: OwnerListViewModel

    lateinit var adapter: OwnerListAdapter

    override fun screenId() = ScreenId.OWNER_LIST

    override suspend fun chainId(): BigInteger? = null

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
            addButton.setOnClickListener {
                findNavController().navigate(OwnerListFragmentDirections.actionOwnerListFragmentToOwnerAddOptionsFragment())
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
                            else -> {
                                Timber.w("Unknown action: $action")
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
        //FIXME: find better way to pass results in nav graph
        if (findNavController().getFromCurrent<Boolean>(SafeOverviewBaseFragment.OWNER_REMOVED_RESULT) == true) {
            snackbar(requireView(), getString(R.string.signing_owner_key_removed))
            findNavController().setToCurrent(SafeOverviewBaseFragment.OWNER_REMOVED_RESULT, false)
        }
        if (findNavController().getFromCurrent<Boolean>(SafeOverviewBaseFragment.OWNER_IMPORT_RESULT) == true) {
            snackbar(requireView(), getString(R.string.signing_owner_key_imported))
            findNavController().setToCurrent(SafeOverviewBaseFragment.OWNER_IMPORT_RESULT, false)
        }

    }

    override fun onOwnerClick(owner: Solidity.Address, type: Owner.Type) {
        findNavController().navigate(OwnerListFragmentDirections.actionOwnerListFragmentToOwnerDetailsFragment(owner.asEthereumAddressString()))
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
