package pm.gnosis.heimdall.data.repositories

import io.reactivex.Completable
import io.reactivex.Observable
import pm.gnosis.heimdall.accounts.base.models.Signature
import java.math.BigInteger

interface SignaturePushRepository {
    fun init()
    fun request(safe: BigInteger, data: String): Completable
    fun observe(safe: BigInteger): Observable<Signature>
}