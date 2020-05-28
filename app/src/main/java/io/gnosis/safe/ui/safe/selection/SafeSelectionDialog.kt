package io.gnosis.safe.ui.safe.selection

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.DialogSafeSelectionBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.BaseBottomSheetDialogFragment
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.SafeOverviewNavigationHandler
import javax.inject.Inject

class SafeSelectionDialog : BaseBottomSheetDialogFragment<DialogSafeSelectionBinding>() {

    @Inject
    lateinit var viewModel: SafeSelectionViewModel

    @Inject
    lateinit var adapter: SafeSelectionAdapter

    var navHandler: SafeOverviewNavigationHandler? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navHandler = context as SafeOverviewNavigationHandler
    }

    override fun onDetach() {
        super.onDetach()
        navHandler = null
    }

    override fun screenId() = ScreenId.SAFE_SELECT

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun viewModelProvider() = this

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): DialogSafeSelectionBinding =
        DialogSafeSelectionBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            list.layoutManager = LinearLayoutManager(context)
            list.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            list.adapter = adapter
        }

        viewModel.state.observe(viewLifecycleOwner, Observer { state ->

            when (state) {
                is SafeSelectionState.SafeListState -> {
                    adapter.setItems(state.listItems, state.activeSafe)
                    navHandler?.setSafeData(state.activeSafe)
                }
                is SafeSelectionState.AddSafeState -> {

                    state.viewAction?.let {
                        val action = it as BaseStateViewModel.ViewAction.NavigateTo
                        findNavController().navigate(action.navDirections)
                        dismiss()
                    }
                }
            }
        })

        viewModel.loadSafes()
    }
}
