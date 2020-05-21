package io.gnosis.safe.ui.safe.empty

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class NoSafeViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SafesState>(appDispatchers) {

    override fun initialState() = SafesState(null, ViewAction.Loading(true))

    init {
        safeLaunch {
            safeRepository.activeSafeFlow().collect { safe ->
                updateState(true) {
                    SafesState(
                        safe, if (safe == null) ViewAction.None else ViewAction.NavigateTo(
                            NoSafeFragmentDirections.actionNoSafeFragmentToSafeBalancesFragment()
                        )
                    )
                }
            }
        }
    }
}

data class SafesState(
    val activeSafe: Safe?,
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

