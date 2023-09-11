package io.gnosis.safe.ui.settings.owner.ledger

import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import java.security.MessageDigest
import javax.inject.Inject

class LedgerSignViewModel
@Inject constructor(
    private val ledgerController: LedgerController,
    private val credentialsRepository: CredentialsRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<LedgerSignState>(appDispatchers) {

    override fun initialState() = LedgerSignState(ViewAction.Loading(true))

    fun getSignature(mode: LedgerSignDialog.Mode, ownerAddress: Solidity.Address, txHash: String) {
        safeLaunch {
            val owner = credentialsRepository.owner(ownerAddress)!!
            val signature = when (mode) {
                LedgerSignDialog.Mode.EXECUTION -> {
                    ledgerController.getTxSignature(
                        owner.keyDerivationPath!!,
                        txHash
                    )
                }
                else -> {
                    ledgerController.getSignature(
                        owner.keyDerivationPath!!,
                        txHash
                    )
                }
            }
            updateState {
                LedgerSignState(Signature(signature))
            }
            updateState {
                LedgerSignState(ViewAction.None)
            }
        }
    }

    fun getPreviewHash(safeTxHash: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(safeTxHash.hexToByteArray())
        val sha256hash = digest.fold("", { str, it -> str + "%02x".format(it) })
        return sha256hash.uppercase()
    }

    fun disconnectFromDevice() {
        ledgerController.teardownConnection()
    }
}

class LedgerSignState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

data class Signature(
    val signature: String
) : BaseStateViewModel.ViewAction
