package pm.gnosis.heimdall.ui.safe.add

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.helpers.AddressStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.GnosisSafeUtils
import pm.gnosis.models.Wei
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsEthereumAddressOrNull
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class AddSafeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountsRepository: AccountsRepository,
    private val addressStore: AddressStore,
    private val repository: GnosisSafeRepository,
    private val tickerRepository: TickerRepository
) : AddSafeContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    private var cachedFiatPrice: Currency? = null

    private var deviceInfo: BigInteger? = null
        set(value) {
            if (field != value) {
                addressStore.clear()
            }
            field = value
        }

    override fun addExistingSafe(name: String, address: String): Observable<Result<Unit>> {
        return Observable.fromCallable {
            checkName(name)
            val parsedAddress = address.hexAsEthereumAddressOrNull()
                    ?: throw SimpleLocalizedException(context.getString(R.string.invalid_ethereum_address))
            parsedAddress to name
        }.flatMap { (address, name) ->
                repository.add(address, name)
                    .andThen(Observable.just(Unit))
                    .onErrorResumeNext(Function { errorHandler.observable(it) })
            }
            .mapToResult()
    }

    override fun deployNewSafe(name: String, overrideGasPrice: Wei?): Observable<Result<Unit>> {
        return Observable.fromCallable {
            checkName(name)
            name
        }.flatMap {
                addressStore.load().flatMapCompletable {
                    // We add 1 owner because the current device will automatically be added as an owner
                    repository.deploy(name, it, GnosisSafeUtils.calculateThreshold(it.size + 1), overrideGasPrice)
                }
                    .andThen(Observable.just(Unit))
                    .onErrorResumeNext(Function { errorHandler.observable(it) })
            }
            .mapToResult()
    }

    override fun observeEstimate(): Observable<Result<GasEstimate>> {
        return addressStore.observe().flatMapSingle {
            // We add 1 owner because the current device will automatically be added as an owner
            repository.estimateDeployCosts(it, GnosisSafeUtils.calculateThreshold(it.size + 1))
                .onErrorResumeNext({ errorHandler.single(it) })
        }.mapToResult()
    }

    override fun observeAdditionalOwners(): Observable<List<BigInteger>> {
        return addressStore.observe()
            .subscribeOn(Schedulers.io())
            .map { it.sorted() }
    }

    override fun loadFiatConversion(wei: Wei) =
        (cachedFiatPrice?.let { Single.just(it) }
                ?: (tickerRepository.loadCurrency().doOnSuccess { cachedFiatPrice = it }))
            .map { it.convert(wei) to it }
            .mapToResult()

    override fun setupDeploy(): Single<BigInteger> =
        accountsRepository.loadActiveAccount()
            .map { it.address }
            .doOnSuccess { deviceInfo = it }

    override fun removeAdditionalOwner(address: BigInteger): Observable<Result<Unit>> =
        Observable.fromCallable {
            addressStore.remove(address)
        }
            .subscribeOn(Schedulers.io())
            .mapToResult()

    override fun addAdditionalOwner(input: String): Observable<Result<Unit>> =
        Observable.fromCallable {
            val address = input.hexAsEthereumAddressOrNull()
            SimpleLocalizedException.assert(address != null, context, R.string.invalid_ethereum_address)
            SimpleLocalizedException.assert(deviceInfo?.let { it != address }
                    ?: false, context, R.string.error_owner_already_added)
            SimpleLocalizedException.assert(!addressStore.contains(address!!), context, R.string.error_owner_already_added)
            addressStore.add(address)
        }
            .subscribeOn(Schedulers.io())
            .mapToResult()


    override fun loadSafeInfo(address: String): Observable<Result<SafeInfo>> =
        Single.fromCallable {
            address.hexAsEthereumAddressOrNull() ?: throw InvalidAddressException()
        }.flatMapObservable {
                repository.loadInfo(it)
            }.mapToResult()

    override fun loadActiveAccount(): Observable<Account> = accountsRepository.loadActiveAccount().toObservable()
        .onErrorResumeNext { t: Throwable -> Timber.d(t); Observable.empty<Account>() }

    private fun checkName(name: String) {
        if (name.isBlank()) throw SimpleLocalizedException(context.getString(R.string.error_blank_name))
    }
}
