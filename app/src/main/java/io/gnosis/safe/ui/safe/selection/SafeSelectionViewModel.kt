package io.gnosis.safe.ui.safe.selection

import androidx.navigation.ActionOnlyNavDirections
import io.gnosis.data.models.Safe
import io.gnosis.safe.R
import io.gnosis.safe.di.Repositories
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.empty.NoSafeFragmentDirections
import javax.inject.Inject


sealed class SafeSelectionState : BaseStateViewModel.State {

    data class SafeListState(
        val listItems: List<Any>,
        val activeSafe: Safe?,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSelectionState()

    data class AddSafeState(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSelectionState()
}

class SafeSelectionViewModel @Inject constructor(
    repositories: Repositories,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSelectionState>(appDispatchers),
    SafeSelectionAdapter.OnSafeSelectionItemClickedListener {

    private val safeRepository = repositories.safeRepository()

    private val items: MutableList<Any> = mutableListOf()
    private var activeSafe: Safe? = null

    override fun initialState(): SafeSelectionState =
        SafeSelectionState.SafeListState(
            listOf(AddSafeHeader()), null, null
        )

    fun loadSafes() {
        safeLaunch {
            val safes = safeRepository.getSafes()
            activeSafe = safeRepository.getActiveSafe()

            items.clear()
            items.add(AddSafeHeader())
            items.addAll(safes)

            updateState { SafeSelectionState.SafeListState(items, activeSafe, null) }
        }
    }

    fun selectSafe(safe: Safe) {
        safeLaunch {
            safeRepository.setActiveSafe(safe)
            updateState { SafeSelectionState.SafeListState(items, safe, null) }
        }
    }

    fun addSafe() {
        safeLaunch {
            updateState {
                SafeSelectionState.AddSafeState(
                    ViewAction.NavigateTo(
                        ActionOnlyNavDirections(R.id.action_to_add_safe_nav)
                    )
                )
            }
        }
    }

    override fun onSafeClicked(safe: Safe) {
        selectSafe(safe)
    }

    override fun onAddSafeClicked() {
        addSafe()
    }
}

