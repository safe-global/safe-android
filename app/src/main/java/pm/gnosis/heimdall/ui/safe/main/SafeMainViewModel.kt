package pm.gnosis.heimdall.ui.safe.main

import android.content.Context
import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.processors.BehaviorProcessor
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString
import javax.inject.Inject

class SafeMainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addressBookRepository: AddressBookRepository,
    private val preferenceManager: PreferencesManager,
    private val safeRepository: GnosisSafeRepository
) : SafeMainContract() {

    private val safeSelectionProcessor = BehaviorProcessor.create<Optional<AbstractSafe>>()

    override fun observeSafes(): Flowable<Result<Adapter.Data<AbstractSafe>>> =
        Flowable.combineLatest(
            safeRepository.observeAllSafes(),
            safeSelectionProcessor.startWith(None),
            BiFunction { safes: List<AbstractSafe>, selection: Optional<AbstractSafe> ->
                selection.toNullable()?.let { safes - it } ?: safes
            }
        )
            .scanToAdapterData()
            .mapToResult()

    override fun loadSelectedSafe(): Single<out AbstractSafe> =
        Single.fromCallable {
            val selectedSafe = preferenceManager.prefs.getString(KEY_SELECTED_SAFE, null)?.asEthereumAddress()
            selectedSafe.toOptional()
        }.flatMap<AbstractSafe> {
            it.toNullable()?.let {
                loadSafe(it)
            } ?: Single.error(NoSuchElementException())
        }.onErrorResumeNext {
            safeRepository.observeAllSafes()
                .map {
                    it.first()
                }
                .firstOrError()
        }
            .doOnError { safeSelectionProcessor.offer(None) }
            .doOnSuccess { safeSelectionProcessor.offer(it.toOptional()) }

    private fun loadSafe(address: Solidity.Address): Single<AbstractSafe> =
        safeRepository.loadSafe(address).map<AbstractSafe> { it }
            .onErrorResumeNext { safeRepository.loadPendingSafe(address) }
            .onErrorResumeNext { safeRepository.loadRecoveringSafe(address) }

    override fun selectSafe(address: Solidity.Address): Single<out AbstractSafe> =
        Single.fromCallable {
            preferenceManager.prefs.edit { putString(KEY_SELECTED_SAFE, address.asEthereumAddressString()) }
        }.flatMap {
            loadSelectedSafe()
        }

    override fun observeSafe(safe: AbstractSafe): Flowable<Pair<String, String>> =
        addressBookRepository.observeAddressBookEntry(safe.address())
            .map { it.name to it.address.shortChecksumString() }

    override fun updateSafeName(safe: AbstractSafe, name: String?): Completable =
        addressBookRepository.updateAddressBookEntry(safe.address(), name ?: context.getString(R.string.default_safe_name))

    override fun removeSafe(safe: AbstractSafe): Completable =
        when (safe) {
            is Safe -> safeRepository.removeSafe(safe.address)
            is PendingSafe -> safeRepository.removePendingSafe(safe.address)
            is RecoveringSafe -> safeRepository.removeRecoveringSafe(safe.address)
        }

    override fun syncWithChromeExtension(address: Solidity.Address) = safeRepository.sendSafeCreationPush(address)

    private fun Solidity.Address.shortChecksumString() =
        asEthereumAddressChecksumString().let { "${it.subSequence(0, 6)}...${it.subSequence(it.length - 6, it.length)}" }

    companion object {
        private const val KEY_SELECTED_SAFE = "safe_main.string.selected_safe"
    }
}
