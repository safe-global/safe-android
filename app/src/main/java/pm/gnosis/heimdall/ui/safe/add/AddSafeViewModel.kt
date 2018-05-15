package pm.gnosis.heimdall.ui.safe.add

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TxExecutorRepository
import pm.gnosis.heimdall.data.repositories.models.FeeEstimate
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.AddressStore
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.GnosisSafeUtils
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.ticker.data.repositories.TickerRepository
import pm.gnosis.ticker.data.repositories.models.Currency
import pm.gnosis.utils.HttpCodes
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.hexAsBigInteger
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

class AddSafeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountsRepository: AccountsRepository,
    private val addressStore: AddressStore,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val tickerRepository: TickerRepository,
    private val txExecutorRepository: TxExecutorRepository
) : AddSafeContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    private var cachedFiatPrice: Currency? = null

    private var deviceInfo: Solidity.Address? = null
        set(value) {
            if (field != value) {
                addressStore.clear()
            }
            field = value
        }

    override fun addExistingSafe(name: String, address: String): Single<Result<Solidity.Address>> {
        return Single.fromCallable {
            checkName(name)
            val parsedAddress = address.asEthereumAddress() ?: throw SimpleLocalizedException(context.getString(R.string.invalid_ethereum_address))
            parsedAddress to name
        }.flatMap { (address, name) ->
            gnosisSafeRepository.addSafe(address, name)
                .andThen(Single.just(address))
                .onErrorResumeNext { errorHandler.single(it) }
        }
            .mapToResult()
    }

    override fun deployNewSafe(name: String): Single<Result<String>> =
        Single.fromCallable {
            checkName(name)
            name
        }.flatMap {
            addressStore.load().flatMap {
                // We add 1 owner because the current device will automatically be added as an owner
                gnosisSafeRepository.deploy(name, it, GnosisSafeUtils.calculateThreshold(it.size + 1))
            }
                .onErrorResumeNext {
                    if (it is HttpException && it.code() == HttpCodes.UNAUTHORIZED)
                        Single.error(IllegalStateException())
                    else
                        errorHandler.single(it)
                }
        }
            .mapToResult()

    override fun deployNewSafe(name: String, additionalOwners: Set<Solidity.Address>): Single<Result<String>> =
        Single.fromCallable {
            checkName(name)
            name
        }
            .flatMap { accountsRepository.loadActiveAccount() }
            .flatMap { gnosisSafeRepository.deploy(name, additionalOwners + it.address, 2) }
            .onErrorResumeNext {
                if (it is HttpException && it.code() == HttpCodes.UNAUTHORIZED) Single.error(IllegalStateException())
                else errorHandler.single(it)
            }
            .mapToResult()

    override fun saveTransactionHash(transactionHash: String, name: String): Completable =
        gnosisSafeRepository.savePendingSafe(transactionHash.hexAsBigInteger(), name)

    override fun observeAdditionalOwners(): Observable<List<Solidity.Address>> {
        return addressStore.observe()
            .subscribeOn(Schedulers.io())
            .map { it.sortedBy { it.value } }
    }

    override fun loadFiatConversion(wei: Wei) =
        (cachedFiatPrice?.let { Single.just(it) } ?: (tickerRepository.loadCurrency().doOnSuccess { cachedFiatPrice = it }))
            .map { it.convert(wei) to it }
            .mapToResult()

    override fun setupDeploy(): Single<Solidity.Address> =
        accountsRepository.loadActiveAccount()
            .map { it.address }
            .doOnSuccess { deviceInfo = it }

    override fun estimateDeploy(): Single<Result<FeeEstimate>> =
        loadSafeDeployTransaction()
            .flatMapObservable {
                txExecutorRepository.estimate(it)
            }
            .map { FeeEstimate(it.first, it.second) }
            .firstOrError()
            .mapToResult()

    override fun removeAdditionalOwner(address: Solidity.Address): Observable<Result<Unit>> =
        Observable.fromCallable { addressStore.remove(address) }
            .subscribeOn(Schedulers.io())
            .mapToResult()

    override fun addAdditionalOwner(input: String): Observable<Result<Unit>> =
        Observable.fromCallable {
            val address = input.asEthereumAddress()
            SimpleLocalizedException.assert(address != null, context, R.string.invalid_ethereum_address)
            SimpleLocalizedException.assert(deviceInfo?.let { it != address } ?: false, context, R.string.error_owner_already_added)
            SimpleLocalizedException.assert(!addressStore.contains(address!!), context, R.string.error_owner_already_added)
            addressStore.add(address)
        }
            .subscribeOn(Schedulers.io())
            .mapToResult()


    override fun loadSafeInfo(address: String): Observable<Result<SafeInfo>> =
        Single.fromCallable { address.asEthereumAddress() ?: throw InvalidAddressException() }
            .flatMapObservable { gnosisSafeRepository.loadInfo(it) }
            .mapToResult()

    override fun loadActiveAccount(): Observable<Account> = accountsRepository.loadActiveAccount().toObservable()
        .onErrorResumeNext { t: Throwable -> Timber.d(t); Observable.empty<Account>() }

    private fun checkName(name: String) {
        if (name.isBlank()) throw SimpleLocalizedException(context.getString(R.string.error_blank_name))
    }

    override fun loadDeployData(name: String): Single<Result<Transaction>> =
        Single.fromCallable { checkName(name);name }
            .flatMap { loadSafeDeployTransaction() }
            .mapToResult()

    private fun loadSafeDeployTransaction() =
        addressStore.load().flatMap {
            // We add 1 owner because the current device will automatically be added as an owner
            gnosisSafeRepository.loadSafeDeployTransaction(it, GnosisSafeUtils.calculateThreshold(it.size + 1))
        }
}
