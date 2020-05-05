package io.gnosis.safe.ui.safe.selection

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.HeimdallApplication
import io.gnosis.safe.databinding.DialogSafeSelectionBinding
import io.gnosis.safe.di.components.DaggerViewComponent
import io.gnosis.safe.di.modules.ViewModule
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

    override fun inject() {
        DaggerViewComponent.builder()
            .viewModule(ViewModule(context!!))
            .applicationComponent(HeimdallApplication[context!!])
            .build()
            .inject(this)
    }


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
                        dismiss()
                        findNavController().navigate(action.navDirections)
                    }
                }
            }
        })

        viewModel.loadSafes()
    }

    companion object {

        private val TAG = SafeSelectionDialog::class.java.simpleName

        fun show(context: Context) {
            val dialog = SafeSelectionDialog()
            dialog.show(
                (context as FragmentActivity).supportFragmentManager,
                TAG
            )
        }
    }
}
