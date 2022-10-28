package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.data.models.Safe
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.data.utils.SemVer
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import java.math.BigInteger
import javax.inject.Inject

class SendAssetReviewViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val safeRepository: SafeRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendAssetReviewState>(appDispatchers) {

    lateinit var activeSafe: Safe
        private set

    private var safeNonce: BigInteger? = null
    private var minSafeNonce: BigInteger? = null
    private var safeTxGas: BigInteger? = null
    private var proposedSafeTxGas: BigInteger? = null

    override fun initialState(): SendAssetReviewState =
        SendAssetReviewState(viewAction = null)

    init {
        safeLaunch {
            activeSafe = safeRepository.getActiveSafe()!!
        }
    }

    fun loadTxEstimationData(
        chainId: BigInteger,
        from: Solidity.Address,
        to: Solidity.Address,
        value: BigInteger
    ) {
        safeLaunch {
            if (safeNonce != null) {
                updateState {
                    SendAssetReviewState(EstimationDataLoaded)
                }
            }
            val txEstimation = transactionRepository.estimateTransaction(
                chainId,
                from,
                to,
                value
            )
            minSafeNonce = txEstimation.currentNonce
            if (safeNonce == null) {
                safeNonce = txEstimation.recommendedNonce
            }
            if (SemVer.parse(activeSafe.version!!, ignoreExtensions = true) < SemVer(1, 3, 0)) {
                proposedSafeTxGas = txEstimation.safeTxGas
                if (safeTxGas == null) {
                    safeTxGas = proposedSafeTxGas
                }
            }
            updateState {
                SendAssetReviewState(EstimationDataLoaded)
            }
        }
    }

    fun onAdvancedParamsEdit() {
        safeLaunch {
            updateState {
                SendAssetReviewState(
                    viewAction = ViewAction.NavigateTo(
                        SendAssetReviewFragmentDirections.actionSendAssetReviewFragmentToEditAdvancedParamsFragment(
                            activeSafe.chain,
                            safeNonce.toString(),
                            minSafeNonce.toString(),
                            safeTxGas?.toString(),
                            proposedSafeTxGas?.toString()
                        )
                    )
                )
            }
            updateState {
                SendAssetReviewState(
                    viewAction = ViewAction.None
                )
            }
        }
    }

    fun updateAdvancedParams(nonce: String?, txGas: String?) {
        nonce?.let {
            safeNonce = BigInteger(it)
        }
        txGas?.let {
            safeTxGas = BigInteger(it)
        }
    }

    fun onConfirm() {
        //TODO: proceed with creating transaction
    }
}

data class SendAssetReviewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State

object EstimationDataLoaded: BaseStateViewModel.ViewAction
