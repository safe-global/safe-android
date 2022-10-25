package io.gnosis.safe.ui.safe.send_funds

import io.gnosis.data.repositories.TransactionRepository
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import pm.gnosis.model.Solidity
import java.math.BigInteger
import javax.inject.Inject

class SendAssetReviewViewModel
@Inject constructor(
    private val transactionRepository: TransactionRepository,
    appDispatchers: AppDispatchers
) : BaseStateViewModel<SendAssetReviewState>(appDispatchers) {

    override fun initialState(): SendAssetReviewState =
        SendAssetReviewState(viewAction = null)

    fun loadTxEstimationData(
        chainId: BigInteger,
        from: Solidity.Address,
        to: Solidity.Address,
        value: BigInteger
    ) {
        safeLaunch {
            transactionRepository.estimateTransaction(
                chainId,
                from,
                to,
                value
            )
        }
    }
}

data class SendAssetReviewState(
    override var viewAction: BaseStateViewModel.ViewAction?
) : BaseStateViewModel.State
