package io.gnosis.safe.ui.settings.owner.ledger

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import javax.inject.Inject

class LedgerSignViewModel
@Inject constructor(
    private val ledgerController: LedgerController,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<LedgerSignState>(appDispatchers) {

    override fun initialState() = LedgerSignState(ViewAction.Loading(true))

    fun getSignature(ownerAddress: Solidity.Address, safeTxHash: String) {
        safeLaunch {
            val owner = credentialsRepository.owner(ownerAddress)!!
            val signature = ledgerController.getSignature(
                owner.keyDerivationPath!!,
                safeTxHash
            )
            ledgerController.teardownConnection()
            updateState {
                LedgerSignState(Signature(signature))
            }
        }
    }
}

class LedgerSignState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class Signature(
    val signature: String
): BaseStateViewModel.ViewAction
