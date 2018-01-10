package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.GnosisSafe.Threshold
import pm.gnosis.heimdall.GnosisSafe.GetOwners
import pm.gnosis.heimdall.ProxyFactory
import pm.gnosis.heimdall.accounts.base.models.Account
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.BulkRequest.SubRequest
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.JsonRpcRequest
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.remote.models.TransactionParameters
import pm.gnosis.heimdall.data.remote.models.TransactionReceipt
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.nullOnThrow
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGnosisSafeRepository @Inject constructor(
        gnosisAuthenticatorDb: GnosisAuthenticatorDb,
        private val accountsRepository: AccountsRepository,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository,
        private val settingsRepository: SettingsRepository
) : GnosisSafeRepository {

    private val safeDao = gnosisAuthenticatorDb.gnosisSafeDao()
    private val descriptionsDao = gnosisAuthenticatorDb.descriptionsDao()

    override fun observeSafes() =
            Flowable.combineLatest(safeDao.observePendingSafes(), safeDao.observeSafes(),
                    BiFunction { pendingSafes: List<PendingGnosisSafeDb>, safes: List<GnosisSafeDb> ->
                        pendingSafes.map { it.fromDb() } + safes.map { it.fromDb() }
                    })
                    .subscribeOn(Schedulers.io())!!

    override fun observeDeployedSafes() =
            safeDao.observeSafes()
                    .map { it.map { it.fromDb() } }
                    .subscribeOn(Schedulers.io())!!

    override fun observeSafe(address: BigInteger): Flowable<Safe> =
            safeDao.observeSafe(address)
                    .subscribeOn(Schedulers.io())
                    .map { it.fromDb() }

    override fun add(address: BigInteger, name: String?) =
            Completable.fromCallable {
                safeDao.insertSafe(GnosisSafeDb(address, name))
            }.subscribeOn(Schedulers.io())!!

    private fun loadSafeDeployParams(devices: Set<BigInteger>, requiredConfirmations: Int): Single<SafeDeployParams> {
        return accountsRepository.loadActiveAccount()
                .flatMapObservable { account ->
                    val factoryAddress = settingsRepository.getProxyFactoryAddress()
                    val masterCopyAddress = settingsRepository.getSafeMasterCopyAddress()
                    val owners = SolidityBase.Vector((devices + account.address).map { Solidity.Address(it) })
                    val confirmations = Math.max(1, Math.min(requiredConfirmations, devices.size))
                    val setupData = GnosisSafe.Setup.encode(
                            // Safe owner info
                            owners, Solidity.UInt8(BigInteger.valueOf(confirmations.toLong())),
                            // Extension info -> not set for now
                            Solidity.Address(BigInteger.ZERO), Solidity.Bytes(ByteArray(0)))
                    val data = ProxyFactory.CreateProxy.encode(Solidity.Address(masterCopyAddress), Solidity.Bytes(setupData.hexStringToByteArray()))
                    ethereumJsonRpcRepository.getTransactionParameters(account.address, TransactionCallParams(to = factoryAddress.asEthereumAddressString(), data = data)).map {
                        SafeDeployParams(account, factoryAddress, data, it)
                    }
                }
                .singleOrError()
    }

    override fun estimateDeployCosts(devices: Set<BigInteger>, requiredConfirmations: Int): Single<GasEstimate> {
        return loadSafeDeployParams(devices, requiredConfirmations)
                .map { GasEstimate(it.transactionParameters.gas, Wei(it.transactionParameters.gasPrice)) }
    }

    override fun deploy(name: String?, devices: Set<BigInteger>, requiredConfirmations: Int, overrideGasPrice: Wei?): Completable {
        return loadSafeDeployParams(devices, requiredConfirmations)
                .flatMap {
                    val params = it.transactionParameters
                    val gasPrice = overrideGasPrice?.value ?: params.gasPrice
                    val transaction = Transaction(address = it.factoryAddress, data = it.data, nonce = params.nonce, gas = params.gas, gasPrice = gasPrice)
                    accountsRepository.signTransaction(transaction)
                }
                .flatMapObservable { ethereumJsonRpcRepository.sendRawTransaction(it) }
                .flatMapCompletable {
                    Completable.fromAction {
                        safeDao.insertPendingSafe(PendingGnosisSafeDb(it.hexAsBigInteger(), name))
                    }
                }
    }

    override fun observeDeployStatus(hash: String): Observable<String> {
        return ethereumJsonRpcRepository.getTransactionReceipt(hash)
                .flatMap {
                    if (it.status != null) {
                        it.logs.forEach {
                            decodeCreationEventOrNull(it)?.let {
                                return@flatMap Observable.just(it.proxy.value)
                            }
                        }
                        Observable.error<BigInteger>(SafeDeploymentFailedException())
                    } else {
                        Observable.error<BigInteger>(IllegalStateException())
                    }
                }
                .retryWhen {
                    it.flatMap {
                        if (it is SafeDeploymentFailedException) {
                            Observable.error(it)
                        } else {
                            Observable.just(it).delay(20, TimeUnit.SECONDS)
                        }
                    }
                }
                .flatMapSingle { safeAddress ->
                    safeDao.loadPendingSafe(hash.hexAsBigInteger()).map { pendingSafe ->
                        safeAddress to pendingSafe
                    }
                }
                .map {
                    safeDao.removePendingSafe(hash.hexAsBigInteger())
                    safeDao.insertSafe(GnosisSafeDb(it.first, it.second.name))
                    it.first.asEthereumAddressString()
                }
                .doOnError {
                    safeDao.removePendingSafe(hash.hexAsBigInteger())
                }
    }

    private fun decodeCreationEventOrNull(event: TransactionReceipt.Event) =
            nullOnThrow { ProxyFactory.Events.ProxyCreation.decode(event.topics, event.data) }

    override fun remove(address: BigInteger) =
            Completable.fromCallable {
                safeDao.removeSafe(address)
            }.subscribeOn(Schedulers.io())!!

    override fun updateName(address: BigInteger, newName: String) =
            Completable.fromCallable {
                safeDao.updateSafe(GnosisSafeDb(address, newName))
            }.subscribeOn(Schedulers.io())!!

    override fun loadInfo(address: BigInteger): Observable<SafeInfo> {
        val addressString = address.asEthereumAddressString()
        return accountsRepository.loadActiveAccount()
                .map {
                    SafeInfoRequest(
                            SubRequest(JsonRpcRequest(
                                    id = 0,
                                    method = EthereumJsonRpcRepository.FUNCTION_GET_BALANCE,
                                    params = arrayListOf(address.asEthereumAddressString(), EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)),
                                    { Wei(it.checkedResult().hexAsBigInteger()) }),
                            SubRequest(TransactionCallParams(to = addressString, data = Threshold.encode()).callRequest(1),
                                    { Threshold.decode(it.checkedResult()) }),
                            SubRequest(TransactionCallParams(to = addressString, data = GetOwners.encode()).callRequest(2),
                                    { GetOwners.decode(it.checkedResult()) }),
                            SubRequest(TransactionCallParams(to = addressString, data = GnosisSafe.IsOwner.encode(Solidity.Address(it.address))).callRequest(3),
                                    { GnosisSafe.IsOwner.decode(it.checkedResult()) }))
                }
                .flatMapObservable { ethereumJsonRpcRepository.bulk(it) }
                .map {
                    SafeInfo(addressString,
                            it.balance.value!!,
                            it.requiredConfirmations.value!!.param0.value.toLong(),
                            it.owners.value!!.param0.items.map { it.value.asEthereumAddressString() },
                            it.isOwner.value!!.param0.value)
                }
    }

    override fun observeTransactionDescriptions(address: BigInteger): Flowable<List<String>> {
        return descriptionsDao.observeDescriptions(address)
    }

    private class SafeInfoRequest(
            val balance: SubRequest<Wei>,
            val requiredConfirmations: SubRequest<Threshold.Return>,
            val owners: SubRequest<GetOwners.Return>,
            val isOwner: SubRequest<GnosisSafe.IsOwner.Return>
    ) : BulkRequest(balance, requiredConfirmations, owners, isOwner)

    private data class SafeDeployParams(val account: Account, val factoryAddress: BigInteger, val data: String, val transactionParameters: TransactionParameters)

    private class SafeDeploymentFailedException() : IllegalArgumentException()
}
