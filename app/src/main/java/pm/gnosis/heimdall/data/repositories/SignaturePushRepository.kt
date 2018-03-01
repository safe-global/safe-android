package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger

interface SignaturePushRepository {
    fun init()
    fun request(safe: BigInteger, data: String): Completable
    fun send(safe: BigInteger, transaction: Transaction, signature: Signature): Completable
    fun observe(safe: BigInteger): Observable<Signature>
    fun receivedSignature(topic: String?, signature: Signature)
}
