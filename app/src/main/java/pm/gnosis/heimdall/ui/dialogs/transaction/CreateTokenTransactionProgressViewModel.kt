package pm.gnosis.heimdall.ui.dialogs.transaction

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.StandardToken
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import java.math.BigInteger
import javax.inject.Inject

class CreateTokenTransactionProgressViewModel @Inject constructor() : CreateTokenTransactionProgressContract() {
    override fun loadCreateTokenTransaction(tokenAddress: Solidity.Address): Single<Transaction> =
        Single.fromCallable {
            // 0x0 indicates a ether transfer
            val data = if (tokenAddress.value == BigInteger.ZERO) null else
                StandardToken.Transfer.encode(Solidity.Address(BigInteger.ZERO), Solidity.UInt256(BigInteger.ZERO))
            Transaction(tokenAddress, data = data)
        }.subscribeOn(Schedulers.computation())
}
