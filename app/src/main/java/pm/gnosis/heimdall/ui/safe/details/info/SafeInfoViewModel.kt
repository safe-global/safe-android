package pm.gnosis.heimdall.ui.safe.details.info

import android.content.Context
import io.reactivex.Observable
import io.reactivex.functions.Function
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import java.math.BigInteger
import javax.inject.Inject

class SafeInfoViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val safeRepository: GnosisSafeRepository
) : SafeInfoContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
            .build()

    private var cachedInfo: SafeInfo? = null

    private var address: BigInteger? = null

    override fun setup(address: BigInteger) {
        if (this.address == address) {
            return
        }
        this.address = address
        cachedInfo = null
    }

    override fun loadSafeInfo(ignoreCache: Boolean) =
            (fromCache(ignoreCache) ?:
                    safeRepository.loadInfo(address!!)
                            .onErrorResumeNext(Function { errorHandler.observable(it) })
                            .doOnNext { cachedInfo = it })
                    .mapToResult()

    private fun fromCache(ignoreCache: Boolean): Observable<SafeInfo>? {
        if (!ignoreCache) {
            return cachedInfo?.let { Observable.just(it) }
        }
        return null
    }
}