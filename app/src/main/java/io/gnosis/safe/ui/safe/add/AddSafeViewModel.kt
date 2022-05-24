package io.gnosis.safe.ui.safe.add

import io.gnosis.data.models.Chain
import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.SafeStatus
import io.gnosis.data.repositories.UnstoppableDomainsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.utils.HttpCodes
import retrofit2.HttpException
import javax.inject.Inject

class AddSafeViewModel
@Inject constructor(
    private val safeRepository: SafeRepository,
    private val unstoppableDomainsRepository: UnstoppableDomainsRepository,
    private val ensRepository: EnsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<BaseStateViewModel.State>(appDispatchers) {

    override fun initialState(): State = AddSafeState(ViewAction.Loading(false))

    fun validate(safe: Safe) {
        safeLaunch {
            updateState { AddSafeState(ViewAction.Loading(true)) }
            takeUnless { safeRepository.isSafeAddressUsed(safe) } ?: throw UsedSafeAddress
            kotlin.runCatching {
                when (safeRepository.getSafeStatus(safe)) {
                    SafeStatus.VALID -> updateState { AddSafeState(ShowValidSafe(safe)) }
                    SafeStatus.NOT_SUPPORTED -> throw SafeNotSupported
                    SafeStatus.INVALID -> throw InvalidSafeAddress
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

    fun enableUD(chain: Chain): Boolean {
        return unstoppableDomainsRepository.canResolve(chain)
    }

    fun enableENS(chain: Chain): Boolean {
        return ensRepository.canResolve(chain)
    }
}


object SafeNotSupported : Throwable()
object SafeNotFound : Throwable()
object InvalidSafeAddress : Throwable()
object UsedSafeAddress : Throwable()
//FIXME: implement proper support for EIP-3770 addresses
object AddressPrefixMismatch: Throwable()

data class ShowValidSafe(
    val safe: Safe
) : BaseStateViewModel.ViewAction

data class AddSafeState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
