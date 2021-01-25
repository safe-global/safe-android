package io.gnosis.safe.ui.settings.owner.list

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentOwnerSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseStateViewModel.ViewAction.CloseScreen
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import io.gnosis.safe.ui.settings.app.AppSettingsFragment.Companion.OWNER_IMPORT_RESULT
import kotlinx.coroutines.launch
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.svalinn.common.utils.visible
import javax.inject.Inject

class OwnerSelectionFragment : BaseViewBindingFragment<FragmentOwnerSelectionBinding>(), OwnerListAdapter.OnOwnerItemClickedListener {

    override fun screenId() = ScreenId.OWNER_SELECT_ACCOUNT

    private val navArgs by navArgs<OwnerSelectionFragmentArgs>()
    private val privateKey: String? by lazy { navArgs.privateKey }
    private val seedPhrase: String? by lazy { navArgs.seedPhrase }

    @Inject
    lateinit var viewModel: OwnerSelectionViewModel

    private lateinit var adapter: OwnerListAdapter


    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentOwnerSelectionBinding =
        FragmentOwnerSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OwnerListAdapter()
        adapter.setListener(this)
        adapter.addLoadStateListener { loadState ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                if (loadState.refresh is LoadState.Loading && adapter.itemCount == 0) {
                    binding.progress.visible(true)
                }

                if (viewModel.state.value?.viewAction is LoadedOwners && loadState.refresh is LoadState.NotLoading && adapter.itemCount == 0) {
                    binding.showMoreOwners.visible(false)
                } else {
                    binding.progress.visible(false)
                    binding.importButton.isEnabled = true
                    binding.showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                }
            }
        }

        with(binding) {
            backButton.setOnClickListener {
                Navigation.findNavController(it).navigateUp()
            }
            importButton.setOnClickListener {
                if (usingSeedPhrase()) {
                    viewModel.importOwner()
                } else {
                    viewModel.importOwner(privateKey)
                }
            }
            owners.adapter = adapter
            owners.layoutManager = LinearLayoutManager(requireContext())
            val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
            dividerItemDecoration.setDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.divider)!!)
            owners.addItemDecoration(dividerItemDecoration)
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->
            state.viewAction.let { viewAction ->
                when (viewAction) {
                    is FirstOwner -> {
                        lifecycleScope.launch {
                            with(binding) {
                                firstOwnerAddress.text = viewAction.owner.asEthereumAddressChecksumString()
                                firstOwnerImage.setAddress(viewAction.owner)
                                if (viewAction.hasMore) {
                                    firstOwnerNumber.text = "#1"
                                } else {
                                    firstOwnerNumber.text = ""
                                }

                                progress.visible(false)
                                importButton.isEnabled = true
                                showMoreOwners.visible(viewAction.hasMore)
                                showMoreOwners.setOnClickListener {
                                    viewModel.loadMoreOwners()
                                }
                            }
                        }
                    }
                    is LoadedOwners -> {
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
                                showMoreOwners.visible(adapter.pagesVisible < MAX_PAGES)
                            }
                        }
                        lifecycleScope.launch {
                            adapter.submitData(viewAction.newOwners)
                        }
                    }
                    is CloseScreen -> {
                        findNavController().popBackStack(R.id.settingsFragment, false)
                        findNavController().currentBackStackEntry?.savedStateHandle?.set(OWNER_IMPORT_RESULT, true)
                    }
                    else -> {
                    }
                }
            }
        })

        if (usingSeedPhrase()) {
            viewModel.loadOwners(seedPhrase!!)
        } else {
            viewModel.loadOwner(privateKey!!)
        }
    }

    private fun usingSeedPhrase(): Boolean = seedPhrase != null

    override fun onOwnerClicked(ownerIndex: Long) {
        viewModel.setOwnerIndex(ownerIndex)
    }

    companion object {
        private const val MAX_PAGES = OwnerPagingProvider.MAX_PAGES
    }
}
