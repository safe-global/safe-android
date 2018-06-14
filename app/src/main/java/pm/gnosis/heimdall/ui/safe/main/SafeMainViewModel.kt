package pm.gnosis.heimdall.ui.safe.main

import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.processors.BehaviorProcessor
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import javax.inject.Inject

class SafeMainViewModel @Inject constructor(
    private val preferenceManager: PreferencesManager,
    private val safeRepository: GnosisSafeRepository
) : SafeMainContract() {

    private val safeSelectionProcessor = BehaviorProcessor.create<Optional<AbstractSafe>>()

    override fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>> =
        Flowable.combineLatest(
            safeRepository.observeSafes(),
            safeSelectionProcessor.startWith(None),
            BiFunction { safes: List<AbstractSafe>, selection: Optional<AbstractSafe> ->
                selection.toNullable()?.let { safes - it } ?: safes
            }
        )
            .scanToAdapterData()
            .mapToResult()

    override fun loadSelectedSafe(): Single<out AbstractSafe> =
        Single.fromCallable {
            val selectedSafe = preferenceManager.prefs.getString(KEY_SELECTED_SAFE, null)?.hexAsBigInteger()
            selectedSafe.toOptional()
        }.flatMap<AbstractSafe> {
            it.toNullable()?.let {
                loadSafe(it)
            } ?: Single.error(NoSuchElementException())
        }.onErrorResumeNext {
            safeRepository.observeSafes()
                .map {
                    it.first()
                }
                .firstOrError()
        }
            .doOnError { safeSelectionProcessor.offer(None) }
            .doOnSuccess { safeSelectionProcessor.offer(it.toOptional()) }

    private fun loadSafe(addressOrHash: BigInteger): Single<AbstractSafe> =
        Single.fromCallable { Solidity.Address(addressOrHash) }
            .flatMap { safeRepository.loadSafe(it).map<AbstractSafe> { it } }
            .onErrorResumeNext { safeRepository.loadPendingSafe(addressOrHash) }

    override fun selectSafe(addressOrHash: BigInteger): Single<out AbstractSafe> =
        Single.fromCallable {
            preferenceManager.prefs.edit { putString(KEY_SELECTED_SAFE, addressOrHash.toHexString()) }
        }.flatMap {
            loadSelectedSafe()
        }

    override fun syncWithChromeExtension(address: Solidity.Address) = safeRepository.sendSafeCreationPush(address)

    companion object {
        private const val KEY_SELECTED_SAFE = "safe_main.string.selected_safe"
    }
}
