package pm.gnosis.heimdall.ui.transactions.create

import androidx.lifecycle.ViewModel
import android.content.Intent
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import java.math.BigInteger

abstract class CreateAssetTransferContract : ViewModel() {
    abstract fun processInput(
        safe: Solidity.Address,
        tokenAddress: Solidity.Address,
        reviewEvents: Observable<Unit>
    ): ObservableTransformer<Input, Result<ViewUpdate>>

    data class Input(val amount: String, val address: Solidity.Address)

    sealed class ViewUpdate {
        data class Estimate(
            val gasToken: ERC20TokenWithBalance,
            val networkFee: BigInteger,
            val assetBalanceAfterTransfer: ERC20TokenWithBalance?, // null if gasToken and assetToken are the same
            val sufficientFunds: Boolean
        ) : ViewUpdate()

        data class EstimateError(val error: Throwable) : ViewUpdate()
        data class TokenInfo(val value: ERC20TokenWithBalance) : ViewUpdate()
        data class InvalidInput(val amount: Boolean, val address: Boolean) : ViewUpdate()
        data class StartReview(val intent: Intent) : ViewUpdate()
    }
}
