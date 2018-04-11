package pm.gnosis.heimdall.ui.safe.details.info

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.di.ApplicationContext
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.trimWhitespace
import javax.inject.Inject

class SafeSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository
) : SafeSettingsContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    private var cachedInfo: SafeInfo? = null

    private var address: Solidity.Address? = null

    override fun setup(address: Solidity.Address) {
        if (this.address == address) return
        this.address = address
        cachedInfo = null
    }

    override fun getSafeAddress(): Solidity.Address = address!!

    override fun loadSafeInfo(ignoreCache: Boolean) =
        (fromCache(ignoreCache) ?: safeRepository.loadInfo(address!!)
            .onErrorResumeNext(Function { errorHandler.observable(it) })
            .doOnNext { cachedInfo = it })
            .mapToResult()

    override fun loadSafeName() =
        safeRepository.observeSafe(address!!)
            .firstOrError()
            .map { it.name ?: "" }
            .onErrorReturnItem("")!!

    override fun updateSafeName(name: String) =
        Single.fromCallable {
            if (name.isBlank()) throw SimpleLocalizedException(context.getString(R.string.error_blank_name))
            name.trimWhitespace()
        }.flatMap { safeRepository.updateName(address!!, it).andThen(Single.just(it)) }
            .mapToResult()

    override fun deleteSafe() =
        safeRepository.removeSafe(address!!)
            .andThen(Single.just(Unit))
            .onErrorResumeNext { throwable: Throwable -> errorHandler.single(throwable) }
            .mapToResult()

    private fun fromCache(ignoreCache: Boolean): Observable<SafeInfo>? {
        if (!ignoreCache) {
            return cachedInfo?.let { Observable.just(it) }
        }
        return null
    }
}
