package io.gnosis.safe.ui.safe.add

import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import javax.inject.Inject

class AddSafeViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    override fun initialState(): State = AddSafeState(ViewAction.Loading(false))

    fun validate(address: Solidity.Address) {
        safeLaunch {
            updateState { AddSafeState(ViewAction.Loading(true)) }
            takeUnless { safeRepository.isSafeAddressUsed(address) } ?: throw UsedSafeAddress
            kotlin.runCatching {
                if (safeRepository.isValidSafe(address)) {
                    updateState { AddSafeState(ShowValidSafe(address)) }
                } else {
                    throw InvalidSafeAddress
                }
            }.onFailure {
                if (it is HttpException && it.code() == HttpCodes.NOT_FOUND) {
                    throw SafeNotFound
                } else {
                    throw it
                }
            }
        }
    }
}

object SafeNotFound : Throwable()
object InvalidSafeAddress : Throwable()
object UsedSafeAddress : Throwable()

data class ShowValidSafe(
    val address: Solidity.Address
) : BaseStateViewModel.ViewAction

data class AddSafeState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
