package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.GnosisSafePersonalEdition.*
import pm.gnosis.heimdall.ProxyFactory
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.PendingGnosisSafeDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.db.models.toDb
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.SettingsRepository
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.heimdall.data.repositories.models.TransactionStatus
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGnosisSafeRepository @Inject constructor(
    gnosisAuthenticatorDb: ApplicationDb,
    private val accountsRepository: AccountsRepository,
    private val ethereumRepository: EthereumRepository,
    private val settingsRepository: SettingsRepository,
    private val pushServiceRepository: PushServiceRepository
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

    override fun observeSafe(address: Solidity.Address): Flowable<Safe> =
        safeDao.observeSafe(address)
            .subscribeOn(Schedulers.io())
            .map { it.fromDb() }

    override fun loadSafe(address: Solidity.Address): Single<Safe> =
        safeDao.loadSafe(address)
            .subscribeOn(Schedulers.io())
            .map { it.fromDb() }

    override fun loadPendingSafe(transactionHash: BigInteger): Single<PendingSafe> =
        safeDao.loadPendingSafe(transactionHash)
            .subscribeOn(Schedulers.io())
            .map { it.fromDb() }

    override fun observePendingSafe(transactionHash: BigInteger): Flowable<PendingSafe> =
        safeDao.observePendingSafe(transactionHash).map { it.fromDb() }

    override fun addSafe(address: Solidity.Address, name: String) =
        Completable.fromCallable {
            safeDao.insertSafe(GnosisSafeDb(address, name))
        }.subscribeOn(Schedulers.io())!!

    private fun loadSafeDeployTransactionWithSender(devices: Set<Solidity.Address>, requiredConfirmations: Int): Single<Pair<Account, Transaction>> =
        accountsRepository.loadActiveAccount()
            .map { account ->
                val factoryAddress = settingsRepository.getProxyFactoryAddress()
                val masterCopyAddress = settingsRepository.getSafeMasterCopyAddress()
                val owners = SolidityBase.Vector(devices.toList() + account.address)
                val confirmations = Math.max(1, Math.min(requiredConfirmations, devices.size))
                val setupData = Setup.encode(
                    // Safe owner info
                    owners, Solidity.UInt8(BigInteger.valueOf(confirmations.toLong())),
                    // Module info -> not set for now
                    Solidity.Address(BigInteger.ZERO), Solidity.Bytes(ByteArray(0))
                )
                val data = ProxyFactory.CreateProxy.encode(masterCopyAddress, Solidity.Bytes(setupData.hexStringToByteArray()))
                account to Transaction(factoryAddress, data = data)
            }

    override fun loadSafeDeployTransaction(devices: Set<Solidity.Address>, requiredConfirmations: Int): Single<Transaction> =
        loadSafeDeployTransactionWithSender(devices, requiredConfirmations).map { it.second }

    override fun savePendingSafe(transactionHash: BigInteger, name: String?, safeAddress: Solidity.Address, payment: Wei): Completable =
        Completable.fromAction {
            safeDao.insertPendingSafe(PendingGnosisSafeDb(transactionHash, name, safeAddress, payment))
        }.subscribeOn(Schedulers.io())

    override fun updatePendingSafe(pendingSafe: PendingSafe): Completable =
        Completable.fromCallable {
            safeDao.updatePendingSafe(pendingSafe.toDb())
        }.subscribeOn(Schedulers.io())

    override fun pendingSafeToDeployedSafe(pendingSafe: PendingSafe): Completable =
        Completable.fromCallable { safeDao.pendingSafeToDeployedSafe(pendingSafe) }
            .andThen(sendSafeCreationPush(pendingSafe.address).onErrorComplete())
            .subscribeOn(Schedulers.io())

    override fun sendSafeCreationPush(safeAddress: Solidity.Address): Completable =
        Single.zip(
            accountsRepository.loadActiveAccount().map { it.address },
            loadInfo(safeAddress).firstOrError().map { it.owners },
            BiFunction<Solidity.Address, List<Solidity.Address>, Set<Solidity.Address>> { deviceAddress, owners -> (owners - deviceAddress).toSet() }
        )
            .flatMapCompletable { pushServiceRepository.propagateSafeCreation(safeAddress, it) }
            .subscribeOn(Schedulers.io())

    override fun removeSafe(address: Solidity.Address) =
        Completable.fromCallable {
            safeDao.removeSafe(address)
        }.subscribeOn(Schedulers.io())!!

    override fun updateSafe(safe: Safe) =
        Completable.fromCallable {
            safeDao.updateSafe(safe.toDb())
        }.subscribeOn(Schedulers.io())!!

    override fun loadInfo(address: Solidity.Address): Observable<SafeInfo> =
        accountsRepository.loadActiveAccount()
            .map {
                SafeInfoRequest(
                    EthBalance(address, 0),
                    EthCall(
                        transaction = Transaction(address = address, data = GetThreshold.encode()),
                        id = 1
                    ),
                    EthCall(
                        transaction = Transaction(address = address, data = GetOwners.encode()),
                        id = 2
                    ),
                    EthCall(
                        transaction = Transaction(
                            address = address,
                            data = IsOwner.encode(it.address)
                        ), id = 3
                    ),
                    EthCall(
                        transaction = Transaction(address = address, data = GetModules.encode()),
                        id = 4
                    )
                )
            }
            .flatMapObservable { bulk -> ethereumRepository.request(bulk) }
            .map {
                val balance = it.balance.result() ?: throw IllegalArgumentException()
                val threshold = GetThreshold.decode(it.threshold.result() ?: throw IllegalArgumentException()).param0.value.toLong()
                val owners = GetOwners.decode(it.owners.result() ?: throw IllegalArgumentException()).param0.items
                val isOwner = IsOwner.decode(it.isOwner.result() ?: throw IllegalArgumentException()).param0.value
                val modules = GetModules.decode(it.modules.result() ?: throw IllegalArgumentException()).param0.items.map { it }
                SafeInfo(address.asEthereumAddressString(), balance, threshold, owners, isOwner, modules)
            }

    override fun observePendingTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>> =
        descriptionsDao.observePendingTransaction(address)
            .subscribeOn(Schedulers.io())
            .map { it.map { TransactionStatus(it.id, it.timestamp, true) } }

    override fun observeSubmittedTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>> =
        descriptionsDao.observeSubmittedTransaction(address)
            .subscribeOn(Schedulers.io())
            .map { it.map { TransactionStatus(it.id, it.timestamp, false) } }

    private class SafeInfoRequest(
        val balance: EthRequest<Wei>,
        val threshold: EthRequest<String>,
        val owners: EthRequest<String>,
        val isOwner: EthRequest<String>,
        val modules: EthRequest<String>
    ) : BulkRequest(balance, threshold, owners, isOwner, modules)

    private class SafeDeploymentFailedException : IllegalArgumentException()
}
