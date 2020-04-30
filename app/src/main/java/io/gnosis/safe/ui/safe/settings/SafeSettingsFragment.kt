package io.gnosis.safe.ui.safe.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.liveData
import io.gnosis.data.models.Safe
import io.gnosis.safe.databinding.FragmentSafeSettingsBinding
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.di.components.ViewComponent
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseFragment
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.SafeOverviewViewModel
import pm.gnosis.svalinn.common.utils.snackbar
import javax.inject.Inject

class SafeSettingsFragment : BaseFragment<FragmentSafeSettingsBinding>() {

    @Inject
    lateinit var safeOverviewViewModel: SafeOverviewViewModel

    @Inject
    lateinit var viewModel: SafeSettingsViewModel

    override fun inject(component: ViewComponent) {
        component.inject(this)
    }

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSafeSettingsBinding =
        FragmentSafeSettingsBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            safeOverviewViewModel.removeSafe()
        }
        viewModel.state.observe(viewLifecycleOwner, Observer {

            when(it) {
                is SafeSettingsState.SafeLoading -> {
                    when(it.viewAction) {
                        is BaseStateViewModel.ViewAction.ShowError -> {
                            snackbar(binding.root, "safe not loaded!")
                        }
                    }

                }
                is SafeSettingsState.SafeSettings -> {
                    binding.name.text = it.name
                }
                is SafeSettingsState.SafeRemoved -> {

                }
            }

        })
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSafe()
    }
}


class SafeSettingsViewModel @Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSettingsState>(appDispatchers) {

    private val safeRepository = repositories.safeRepository()

    private lateinit var safe: Safe

    override val state: LiveData<SafeSettingsState> = liveData {
        for (event in stateChannel.openSubscription())
            emit(event)
    }

    override fun initialState(): SafeSettingsState = SafeSettingsState.SafeLoading(null)

    fun loadSafe() {

        safeLaunch {

            safe = safeRepository.getActiveSafe()!!

            updateState { SafeSettingsState.SafeSettings(safe.localName, null) }
        }
    }
}

sealed class SafeSettingsState : BaseStateViewModel.State {

    data class SafeLoading(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSettingsState()

    data class SafeSettings(
        val name: String,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSettingsState()

    data class SafeRemoved(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSettingsState()
}

