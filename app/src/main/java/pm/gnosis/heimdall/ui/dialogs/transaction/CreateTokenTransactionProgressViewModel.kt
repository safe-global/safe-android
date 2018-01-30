package pm.gnosis.heimdall.ui.dialogs.transaction

import android.content.Context
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.utils.isValidEthereumAddress
import java.math.BigInteger
import javax.inject.Inject


class CreateTokenTransactionProgressViewModel @Inject constructor(
        @ApplicationContext private val context: Context
) : CreateTokenTransactionProgressContract() {
    override fun loadCreateTokenTransaction(tokenAddress: BigInteger?): Single<Transaction> =
            Single.fromCallable {
                if (tokenAddress?.isValidEthereumAddress() != true) {
                    throw SimpleLocalizedException(context.getString(R.string.error_invalid_token_address))
                }
                // 0x0 indicates a ether transfer
                val data = if (tokenAddress == BigInteger.ZERO) null else
                    StandardToken.Transfer.encode(Solidity.Address(BigInteger.ZERO), Solidity.UInt256(BigInteger.ZERO))
                Transaction(tokenAddress, data = data)
            }.subscribeOn(Schedulers.computation())

}