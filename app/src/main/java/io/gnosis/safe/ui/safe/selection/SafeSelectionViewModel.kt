package io.gnosis.safe.ui.safe.selection

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
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
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSelectionState>(appDispatchers),
    SafeSelectionAdapter.OnSafeSelectionItemClickedListener {

    private val items: MutableList<Any> = mutableListOf()
    private var activeSafe: Safe? = null

    override fun initialState(): SafeSelectionState =
        SafeSelectionState.SafeListState(
            listOf(AddSafeHeader), null, null
        )

    fun loadSafes() {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()

            with(items) {
                clear()
                add(AddSafeHeader)
                activeSafe?.let(::add)
                addAll(safeRepository.getSafes().filter { it != activeSafe }.reversed())
            }

            updateState { SafeSelectionState.SafeListState(items, activeSafe, null) }
        }
    }

    private fun selectSafe(safe: Safe) {
        safeLaunch {
            if (safeRepository.getActiveSafe() != safe) {
                safeRepository.setActiveSafe(safe)
                updateState { SafeSelectionState.SafeListState(items, safe, ViewAction.CloseScreen) }
            }
        }
    }

    private fun addSafe() {
        safeLaunch {
            updateState {
                SafeSelectionState.AddSafeState(
                    ViewAction.NavigateTo(
                        SafeSelectionDialogDirections.actionSafeSelectionDialogToAddSafe()
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
