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
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.preferences.PreferencesSafe
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.BridgeRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.AbstractSafe
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.RecoveringSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.base.Adapter
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.heimdall.utils.scanToAdapterData
import pm.gnosis.heimdall.utils.shortChecksumString
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import javax.inject.Inject

class SafeMainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val addressBookRepository: AddressBookRepository,
    private val bridgeRepository: BridgeRepository,
    private val prefs: PreferencesSafe,
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
            val selectedSafe = prefs.selectedSafe
            selectedSafe.toOptional()
        }.flatMap<AbstractSafe> {
            it.toNullable()?.let {
                loadSafe(it)
            } ?: Single.error(NoSuchElementException())
        }.onErrorResumeNext {
            safeRepository.observeAllSafes()
                .map { safes -> safes.first() }
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
            prefs.selectedSafe = address
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

    override fun shouldShowWalletConnectIntro(): Single<Boolean> =
        bridgeRepository.shouldShowIntro()

    override fun loadSafeConfig(safe: AbstractSafe): Single<Result<Pair<Solidity.Address?, Boolean>>> =
        if (safe is Safe)
            safeRepository.observePendingTransactions(safe.address).filter {
                // Wait with check until all transactions are done, in case of pending update transaction
                it.isEmpty()
            }.firstOrError()
                .flatMap { safeRepository.checkSafe(safe.address).firstOrError() }
                .map { (masterCopy, isExtensionConnected) ->
                    SafeContractUtils.checkForUpdate(masterCopy) to isExtensionConnected
                }
                .mapToResult()
        else Single.just<Pair<Solidity.Address?, Boolean>>(null to false).mapToResult()


    override fun syncWithChromeExtension(address: Solidity.Address) = safeRepository.sendSafeCreationPush(address)
}
