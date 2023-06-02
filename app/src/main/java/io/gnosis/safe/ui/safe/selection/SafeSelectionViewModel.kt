package io.gnosis.safe.ui.safe.selection

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.ChainInfoRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.safe.selection.SafeSelectionViewData.*
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.settings.chain.ChainSelectionMode
import javax.inject.Inject

sealed class SafeSelectionState : BaseStateViewModel.State {

    data class SafeListState(
        val listItems: List<SafeSelectionViewData>,
        val activeSafe: Safe?,
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSelectionState()

    data class AddSafeState(
        override var viewAction: BaseStateViewModel.ViewAction?
    ) : SafeSelectionState()
}

class SafeSelectionViewModel @Inject constructor(
    private val safeRepository: SafeRepository,
    private val chainInfoRepository: ChainInfoRepository,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafeSelectionState>(appDispatchers),
    SafeSelectionAdapter.OnSafeSelectionItemClickedListener {

    private val items: MutableList<SafeSelectionViewData> = mutableListOf()
    private var activeSafe: Safe? = null

    override fun initialState(): SafeSelectionState =
        SafeSelectionState.SafeListState(
            listOf(AddSafeHeader), null, null
        )

    fun isChainPrefixPrependEnabled(): Boolean = settingsHandler.chainPrefixPrepend

    fun loadSafes() {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()

            with(items) {
                clear()
                add(AddSafeHeader)
                activeSafe?.let {
                    add(ChainHeader(it.chain.name, it.chain.backgroundColor))
                    add(SafeItem(it))
                    val otherSafesFromChain = safeRepository.getSafesForChain(it.chainId)
                    addAll(otherSafesFromChain.filter { it != activeSafe }.reversed().map { SafeItem(it) })
                }
                val chains = chainInfoRepository.getChains().filter { it.chainId != activeSafe?.chainId }.sortedBy { it.chainId }
                chains.forEach {
                    val safesForChain = safeRepository.getSafesForChain(it.chainId)
                    if (safesForChain.isNotEmpty()) {
                        add(ChainHeader(it.name, it.backgroundColor))
                        addAll(safesForChain.reversed().map { SafeItem(it) })
                    }
                }
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
                        SafeSelectionDialogDirections.actionSafeSelectionDialogToAddSafe(ChainSelectionMode.ADD_SAFE)
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
