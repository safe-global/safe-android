package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import pm.gnosis.ethereum.*
import pm.gnosis.ethereum.models.TransactionParameters
import pm.gnosis.ethereum.models.TransactionReceipt
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.GnosisSafe.GetOwners
import pm.gnosis.heimdall.GnosisSafe.Threshold
import pm.gnosis.heimdall.GnosisSafe.IsOwner
import pm.gnosis.heimdall.ProxyFactory
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.data.repositories.models.GasEstimate
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
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
    gnosisAuthenticatorDb: ApplicationDb,
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository,
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

    private fun loadSafeDeployParams(
        devices: Set<BigInteger>,
        requiredConfirmations: Int
    ): Single<SafeDeployParams> {
        return accountsRepository.loadActiveAccount()
            .flatMapObservable { account ->
                val factoryAddress = settingsRepository.getProxyFactoryAddress()
                val masterCopyAddress = settingsRepository.getSafeMasterCopyAddress()
                val owners =
                    SolidityBase.Vector((devices + account.address).map { Solidity.Address(it) })
                val confirmations = Math.max(1, Math.min(requiredConfirmations, devices.size))
                val setupData = GnosisSafe.Setup.encode(
                    // Safe owner info
                    owners, Solidity.UInt8(BigInteger.valueOf(confirmations.toLong())),
                    // Extension info -> not set for now
                    Solidity.Address(BigInteger.ZERO), Solidity.Bytes(ByteArray(0))
                )
                val data = ProxyFactory.CreateProxy.encode(
                    Solidity.Address(masterCopyAddress),
                    Solidity.Bytes(setupData.hexStringToByteArray())
                )
                ethereumRepository.getTransactionParameters(
                    account.address,
                    factoryAddress,
                    data = data
                ).map {
                    SafeDeployParams(account, factoryAddress, data, it)
                }
            }
            .singleOrError()
    }

    override fun estimateDeployCosts(
        devices: Set<BigInteger>,
        requiredConfirmations: Int
    ): Single<GasEstimate> {
        return loadSafeDeployParams(devices, requiredConfirmations)
            .map {
                GasEstimate(
                    it.transactionParameters.gas,
                    Wei(it.transactionParameters.gasPrice)
                )
            }
    }

    override fun deploy(
        name: String?,
        devices: Set<BigInteger>,
        requiredConfirmations: Int,
        overrideGasPrice: Wei?
    ): Completable {
        return loadSafeDeployParams(devices, requiredConfirmations)
            .flatMap {
                val params = it.transactionParameters
                val gasPrice = overrideGasPrice?.value ?: params.gasPrice
                val transaction =
                    Transaction(
                        address = it.factoryAddress,
                        data = it.data,
                        nonce = params.nonce,
                        gas = params.gas,
                        gasPrice = gasPrice
                    )
                accountsRepository.signTransaction(transaction)
            }
            .flatMapObservable { ethereumRepository.sendRawTransaction(it) }
            .flatMapCompletable {
                Completable.fromAction {
                    safeDao.insertPendingSafe(PendingGnosisSafeDb(it.hexAsBigInteger(), name))
                }
            }
    }

    override fun observeDeployStatus(hash: String): Observable<String> {
        return ethereumRepository.getTransactionReceipt(hash)
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
                    EthBalance(address, 0),
                    EthCall(
                        transaction = Transaction(address = address, data = Threshold.encode()),
                        id = 1
                    ),
                    EthCall(
                        transaction = Transaction(address = address, data = GetOwners.encode()),
                        id = 2
                    ),
                    EthCall(
                        transaction = Transaction(
                            address = address,
                            data = GnosisSafe.IsOwner.encode(Solidity.Address(it.address))
                        ), id = 3
                    )
                )
            }
            .flatMapObservable { bulk ->
                ethereumRepository.request(bulk)
            }
            .map {
                val balance = it.balance.result() ?: throw IllegalArgumentException()
                val threshold =
                    Threshold.decode(it.threshold.result() ?: throw IllegalArgumentException())
                        .param0.value.toLong()
                val owners =
                    GetOwners.decode(it.owners.result() ?: throw IllegalArgumentException())
                        .param0.items.map { it.value }
                val isOwner =
                    IsOwner.decode(it.isOwner.result() ?: throw IllegalArgumentException())
                        .param0.value
                SafeInfo(addressString, balance, threshold, owners, isOwner)
            }
    }

    override fun observeTransactionDescriptions(address: BigInteger): Flowable<List<String>> {
        return descriptionsDao.observeDescriptions(address)
    }

    private class SafeInfoRequest(
        val balance: EthRequest<Wei>,
        val threshold: EthRequest<String>,
        val owners: EthRequest<String>,
        val isOwner: EthRequest<String>
    ): BulkRequest(balance, threshold, owners, isOwner)

    private data class SafeDeployParams(
        val account: Account,
        val factoryAddress: BigInteger,
        val data: String,
        val transactionParameters: TransactionParameters
    )

    private class SafeDeploymentFailedException() : IllegalArgumentException()
}
