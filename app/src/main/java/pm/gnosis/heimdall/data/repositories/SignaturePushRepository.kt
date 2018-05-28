package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger

interface SignaturePushRepository {
    fun init()
    fun request(safe: Solidity.Address, data: String): Completable
    fun send(
        safe: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        signature: Signature
    ): Completable

    fun observe(safe: Solidity.Address): Observable<Signature>
    fun receivedSignature(topic: String?, signature: Signature)
}
