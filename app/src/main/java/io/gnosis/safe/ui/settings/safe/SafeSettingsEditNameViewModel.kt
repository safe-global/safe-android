package io.gnosis.safe.ui.settings.safe

import androidx.lifecycle.viewModelScope
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class SafeSettingsEditNameViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<EditNameState>(appDispatchers) {

    override fun initialState() = EditNameState(null, ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState { EditNameState(safe?.localName, ViewAction.None) }
            }
        }
    }

    fun saveLocalName(name: String) {
        viewModelScope.launch {
            val safe = safeRepository.getActiveSafe()
            safe?.let {
                safeRepository.saveSafe(it.copy(localName = name))
            }
        }
    }
}

data class EditNameState(
    val name: String?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
