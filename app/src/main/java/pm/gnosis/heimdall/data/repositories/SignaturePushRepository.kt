package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature

interface SignaturePushRepository {
    fun init()
    fun request(safe: Solidity.Address, data: String): Completable
    fun send(safe: Solidity.Address, transaction: Transaction, signature: Signature): Completable
    fun observe(safe: Solidity.Address): Observable<Signature>
    fun receivedSignature(topic: String?, signature: Signature)
}
