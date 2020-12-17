package io.gnosis.safe.ui.settings.app.fiat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.gnosis.safe.R
import io.gnosis.safe.ScreenId
import io.gnosis.safe.databinding.FragmentAppFiatBinding
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.errorSnackbar
import io.gnosis.safe.toError
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.base.fragment.BaseViewBindingFragment
import pm.gnosis.svalinn.common.utils.visible
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class AppFiatFragment : BaseViewBindingFragment<FragmentAppFiatBinding>() {

    @Inject
    lateinit var viewModel: AppFiatViewModel

    private val adapter by lazy { FiatListAdapter() }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAppFiatBinding =
        FragmentAppFiatBinding.inflate(inflater, container, false)

    override fun screenId(): ScreenId = ScreenId.SETTINGS_APP_FIAT

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            backButton.setOnClickListener { findNavController().navigateUp() }

            refresh.setOnRefreshListener { viewModel.fetchSupportedFiatCodes() }

            adapter.clickListener = { fiatCode: String -> viewModel.selectedFiatCodeChanged(fiatCode) }
            fiatList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            fiatList.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
            fiatList.adapter = adapter
        }

        viewModel.fetchSupportedFiatCodes()
    }

    override fun onResume() {
        super.onResume()
        viewModel.state().observe(viewLifecycleOwner, Observer {
            when (val viewAction = it.viewAction) {
                is SelectFiat -> (binding.fiatList.adapter as FiatListAdapter).selectedItem = viewAction.fiatCode
                is FiatList -> {
                    binding.progress.visible(false)
                    binding.refresh.isRefreshing = false
                    adapter.setItems(viewAction.fiatCodes)
                    viewModel.fetchDefaultUserFiat()
                }
                is BaseStateViewModel.ViewAction.ShowError -> {
                    binding.progress.visible(false)
                    binding.refresh.isRefreshing = false
                    with(viewAction.error) {
                        Timber.e(this)
                        errorSnackbar(
                            requireView(),
                            toError().message(
                                requireContext(),
                                R.string.error_description_fiat
                            )
                        )
                    }
                }
            }
        })
    }
}
