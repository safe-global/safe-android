package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.data.models.Chain
import io.gnosis.data.repositories.EnsRepository
import io.gnosis.data.repositories.UnstoppableDomainsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import javax.inject.Inject

class SendAssetViewModel
@Inject constructor(
    private val unstoppableDomainsRepository: UnstoppableDomainsRepository,
    private val ensRepository: EnsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendAssetState>(appDispatchers) {

    override fun initialState(): SendAssetState =
        SendAssetState(viewAction = null)

    fun enableUD(chain: Chain): Boolean {
        return unstoppableDomainsRepository.canResolve(chain)
    }

    fun enableENS(chain: Chain): Boolean {
        return ensRepository.canResolve(chain)
    }
}

data class SendAssetState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
