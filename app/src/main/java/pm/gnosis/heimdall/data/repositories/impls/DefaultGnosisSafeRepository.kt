package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.*
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.GnosisSafe.*
import pm.gnosis.heimdall.Proxy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.db.models.*
import pm.gnosis.heimdall.data.remote.RelayServiceApi
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreation
import pm.gnosis.heimdall.data.remote.models.RelaySafeCreationParams
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.*
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexToByteArray
import pm.gnosis.utils.removeHexPrefix
import pm.gnosis.utils.toBytes
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGnosisSafeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    gnosisAuthenticatorDb: ApplicationDb,
    private val accountsRepository: AccountsRepository,
    private val addressBookRepository: AddressBookRepository,
    private val ethereumRepository: EthereumRepository,
    private val pushServiceRepository: PushServiceRepository,
    private val relayServiceApi: RelayServiceApi,
    private val tokenRepository: TokenRepository
) : GnosisSafeRepository {

    private val safeDao = gnosisAuthenticatorDb.gnosisSafeDao()
    private val descriptionsDao = gnosisAuthenticatorDb.descriptionsDao()

    override fun observeAllSafes() =
        Flowable.combineLatest(
            safeDao.observePendingSafes(),
            safeDao.observeRecoveringSafes(),
            safeDao.observeSafes(),
            Function3 { pendingSafes: List<PendingGnosisSafeDb>, recoveringSafes: List<RecoveringGnosisSafeDb>, safes: List<GnosisSafeDb> ->
                pendingSafes.map { it.fromDb() } + recoveringSafes.map { it.fromDb() } + safes.map { it.fromDb() }
            })
            .subscribeOn(Schedulers.io())

    /*
     * Safe deployment
     */
    override fun triggerSafeDeployment(owners: List<Solidity.Address>, threshold: Int) =
        tokenRepository.loadPaymentToken()
            .flatMap { paymentToken ->
                val request = RelaySafeCreationParams(
                    owners = owners,
                    threshold = threshold,
                    saltNonce = System.nanoTime(),
                    paymentToken = paymentToken.address
                )
                relayServiceApi.safeCreation(request).map {
                    assertResponse(request, it)
                    SafeDeployment(it.safe, it.paymentToken, it.payment)
                }
            }


    private fun assertResponse(request: RelaySafeCreationParams, response: RelaySafeCreation): RelaySafeCreation {
        val paymentToken = response.paymentToken
        if (request.paymentToken != paymentToken)
            throw IllegalStateException("Unexpected payment token returned")
        if (response.masterCopy != MASTER_COPY_ADDRESS)
            throw IllegalStateException("Unexpected master copy returned")
        if (response.proxyFactory != PROXY_FACTORY_ADDRESS)
            throw IllegalStateException("Unexpected proxy factory returned")
        if (response.paymentReceiver != TX_ORIGIN_ADDRESS && response.paymentReceiver != FUNDER_ADDRESS)
            throw IllegalStateException("Unexpected payment receiver returned")

        // Web3py seems to add an empty body for empty data, I would say this is a bug
        // see https://github.com/gnosis/bivrost-kotlin/issues/49
        val setupData = Setup.encode(
            _owners = SolidityBase.Vector(request.owners),
            _threshold = Solidity.UInt256(request.threshold.toBigInteger()),
            to = Solidity.Address(BigInteger.ZERO),
            data = Solidity.Bytes(byteArrayOf()),
            payment = Solidity.UInt256(response.payment),
            paymentToken = response.paymentToken,
            paymentReceiver = response.paymentReceiver
        ) + "0000000000000000000000000000000000000000000000000000000000000000"

        if (setupData != response.setupData)
            throw IllegalStateException("Unexpected setup data returned")

        val setupDataHash = Sha3Utils.keccak(setupData.hexToByteArray())
        val salt = Sha3Utils.keccak(setupDataHash + Solidity.UInt256(request.saltNonce.toBigInteger()).encode().hexToByteArray())


        val deploymentCode = PROXY_CODE + MASTER_COPY_ADDRESS.encode()
        val codeHash = Sha3Utils.keccak(deploymentCode.hexToByteArray())
        val create2Hash = Sha3Utils.keccak(byteArrayOf(0xff.toByte()) + PROXY_FACTORY_ADDRESS.value.toBytes(20) + salt + codeHash)
        val address = Solidity.Address(BigInteger(1, create2Hash.copyOfRange(12, 32)))

        if (address != response.safe)
            throw IllegalStateException("Unexpected safe address returned")
        return response
    }

    /*
     * Status
     */

    override fun loadInfo(address: Solidity.Address): Observable<SafeInfo> =
        accountsRepository.signingOwner(address).map { it.address }
            .onErrorReturnItem(Solidity.Address(BigInteger.ZERO)) // If we don't have an owner for this Safe we do a fallback to address 0x0
            .map { safeOwner ->
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
                            data = IsOwner.encode(owner = safeOwner)
                        ), id = 3
                    ),
                    EthCall(
                        transaction = Transaction(address = address, data = GetModules.encode()),
                        id = 4
                    ),
                    EthCall(
                        transaction = Transaction(address = address, data = VERSION.encode()),
                        id = 5
                    )
                )
            }
            .flatMapObservable { bulk -> ethereumRepository.request(bulk) }
            .map { info ->
                val balance = info.balance.result() ?: throw IllegalArgumentException()
                val threshold = GetThreshold.decode(info.threshold.result() ?: throw IllegalArgumentException()).param0.value.toLong()
                val owners = GetOwners.decode(info.owners.result() ?: throw IllegalArgumentException()).param0.items
                val isOwner = IsOwner.decode(info.isOwner.result() ?: throw IllegalArgumentException()).param0.value
                val modules = GetModules.decode(info.modules.result() ?: throw IllegalArgumentException()).param0.items.map { it }
                val version = VERSION.decode(info.version.result() ?: throw IllegalArgumentException()).param0.value.toSemVer()
                SafeInfo(address, balance, threshold, owners, isOwner, modules, version)
            }

    override fun checkSafe(address: Solidity.Address): Observable<Pair<Boolean, Boolean>> =
        ethereumRepository.request(
            CheckSafeRequest(
                masterCopy = EthGetStorageAt(from = address, location = BigInteger.ZERO, id = 0),
                threshold = EthCall(transaction = Transaction(address = address, data = GetThreshold.encode()), id = 1)
            )
        )
            .map { r ->
                r.masterCopy.result().let {
                    !it?.removeHexPrefix().isNullOrBlank() &&
                            SUPPORTED_SAFE_MASTER_COPIES.contains(Proxy.Implementation.decode(it!!).param0)
                } to r.threshold.result().let {
                    !it?.removeHexPrefix().isNullOrBlank() &&
                            GetThreshold.decode(it!!).param0.value > NO_EXTENSION_THRESHOLD
                }
            }

    override fun loadAbstractSafe(address: Solidity.Address): Single<AbstractSafe> =
        loadSafe(address).map { it as AbstractSafe }
            .onErrorResumeNext {
                loadPendingSafe(address)
            }
            .onErrorResumeNext {
                loadRecoveringSafe(address)
            }

    /*
     * Deployed Safes
     */

    override fun observeSafes() =
        safeDao.observeSafes()
            .map { it.map { i -> i.fromDb() } }
            .subscribeOn(Schedulers.io())

    override fun observeSafe(address: Solidity.Address): Flowable<Safe> =
        safeDao.observeSafe(address)
            .subscribeOn(Schedulers.io())
            .map { it.fromDb() }

    override fun loadSafe(address: Solidity.Address): Single<Safe> =
        safeDao.loadSafe(address)
            .subscribeOn(Schedulers.io())
            .map { it.fromDb() }

    override fun addSafe(address: Solidity.Address, name: String?): Completable =
        Completable.fromCallable {
            safeDao.insertSafe(GnosisSafeDb(address))
        }.subscribeOn(Schedulers.io())
            .andThen(addSafeName(address, name))

    override fun removeSafe(address: Solidity.Address) =
        Completable.fromCallable {
            safeDao.removeSafe(address)
            safeDao.removeSafeInfo(address)
        }
            .subscribeOn(Schedulers.io())

    override fun updateSafe(safe: Safe) =
        Completable.fromCallable {
            safeDao.updateSafe(safe.toDb())
        }.subscribeOn(Schedulers.io())

    /*
     * Pending Safes
     */

    override fun observePendingSafe(address: Solidity.Address): Flowable<PendingSafe> =
        safeDao.observePendingSafe(address).map { it.fromDb() }

    override fun loadPendingSafe(address: Solidity.Address): Single<PendingSafe> =
        safeDao.loadPendingSafe(address)
            .subscribeOn(Schedulers.io())
            .map { it.fromDb() }

    override fun addPendingSafe(
        address: Solidity.Address, name: String?, payment: BigInteger, paymentToken: Solidity.Address
    ): Completable =
        Completable.fromAction {
            safeDao.insertPendingSafe(PendingGnosisSafeDb(address, BigInteger.ZERO, paymentToken, payment))
        }
            .subscribeOn(Schedulers.io())
            .andThen(addSafeName(address, name))

    override fun updatePendingSafe(pendingSafe: PendingSafe): Completable =
        Completable.fromCallable {
            safeDao.updatePendingSafe(pendingSafe.toDb())
        }.subscribeOn(Schedulers.io())

    override fun removePendingSafe(address: Solidity.Address) =
        Completable.fromCallable {
            safeDao.removePendingSafe(address)
            safeDao.removeSafeInfo(address)
        }
            .subscribeOn(Schedulers.io())

    /*
     * Recovering Safes
     */

    override fun observeRecoveringSafe(address: Solidity.Address): Flowable<RecoveringSafe> =
        safeDao.observeRecoveringSafe(address).map { it.fromDb() }

    override fun loadRecoveringSafe(address: Solidity.Address): Single<RecoveringSafe> =
        safeDao.loadRecoveringSafe(address)
            .subscribeOn(Schedulers.io())
            .map { it.fromDb() }

    override fun addRecoveringSafe(
        safeAddress: Solidity.Address,
        transactionHash: BigInteger?,
        name: String?,
        executeInfo: TransactionExecutionRepository.ExecuteInformation,
        signatures: List<Signature>
    ): Completable =
        Completable.fromAction {
            if (safeAddress != executeInfo.transaction.wrapped.address &&
                BuildConfig.MULTI_SEND_ADDRESS.asEthereumAddress()!! != executeInfo.transaction.wrapped.address
            ) throw IllegalArgumentException("Transaction target needs to be the safe or multi send")
            safeDao.insertRecoveringSafe(
                RecoveringSafe(
                    safeAddress,
                    transactionHash,
                    executeInfo.transaction.wrapped.address,
                    executeInfo.transaction.wrapped.data!!,
                    executeInfo.txGas,
                    executeInfo.dataGas,
                    executeInfo.operationalGas,
                    executeInfo.gasToken,
                    executeInfo.gasPrice,
                    executeInfo.transaction.wrapped.nonce!!,
                    executeInfo.transaction.operation,
                    signatures
                ).toDb()
            )
        }
            .subscribeOn(Schedulers.io())
            .andThen(addSafeName(safeAddress, name))

    override fun updateRecoveringSafe(recoveringSafe: RecoveringSafe): Completable =
        Completable.fromCallable {
            safeDao.updateRecoveringSafe(recoveringSafe.toDb())
        }.subscribeOn(Schedulers.io())

    override fun removeRecoveringSafe(address: Solidity.Address) =
        Completable.fromCallable {
            safeDao.removeRecoveringSafe(address)
            safeDao.removeSafeInfo(address)
            // TODO: remove transactions from descriptions DAO
        }
            .subscribeOn(Schedulers.io())

    /*
     * Safe creation
     */

    private fun addSafeName(safeAddress: Solidity.Address, name: String?): Completable =
        getSafeName(safeAddress, name)
            .flatMapCompletable { addressBookRepository.addAddressBookEntry(safeAddress, it, "") }

    private fun getSafeName(safeAddress: Solidity.Address, name: String?): Single<String> =
        if (name.isNullOrBlank())
            safeDao.loadTotalSafeCount()
                .subscribeOn(Schedulers.io())
                .map { safeCount ->
                    StringBuilder(context.getString(R.string.default_safe_name)).apply {
                        if (safeCount > 1) {
                            append(safeAddress.asEthereumAddressChecksumString().let { " ${it.subSequence(it.length - 4, it.length)}" })
                        }
                    }.toString()
                }
        else
            Single.just(name)


    override fun pendingSafeToDeployedSafe(pendingSafe: PendingSafe): Completable =
        Single.fromCallable { safeDao.pendingSafeToDeployedSafe(pendingSafe.address) }
            .flatMapCompletable { if (it) sendSafeCreationPush(pendingSafe.address).onErrorComplete() else Completable.complete() }
            .subscribeOn(Schedulers.io())

    override fun recoveringSafeToDeployedSafe(recoveringSafe: RecoveringSafe): Completable =
        Completable.fromCallable { safeDao.recoveringSafeToDeployedSafe(recoveringSafe.address) }
            .andThen(sendSafeCreationPush(recoveringSafe.address).onErrorComplete())
            .subscribeOn(Schedulers.io())

    override fun sendSafeCreationPush(safeAddress: Solidity.Address): Completable =
        Single.zip(
            accountsRepository.signingOwner(safeAddress),
            loadInfo(safeAddress).firstOrError().map { it.owners },
            BiFunction<AccountsRepository.SafeOwner, List<Solidity.Address>, Set<Solidity.Address>> { signingOwner, owners ->
                (owners - signingOwner.address).toSet()
            }
        )
            .flatMapCompletable { pushServiceRepository.propagateSafeCreation(safeAddress, it) }
            .subscribeOn(Schedulers.io())

    /*
     * Safe Transactions
     */

    override fun observePendingTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>> =
        descriptionsDao.observePendingTransaction(address)
            .subscribeOn(Schedulers.io())
            .map { it.map { tx -> TransactionStatus(tx.id, tx.timestamp, true) } }

    override fun observeSubmittedTransactions(address: Solidity.Address): Flowable<List<TransactionStatus>> =
        descriptionsDao.observeSubmittedTransaction(address)
            .subscribeOn(Schedulers.io())
            .map { it.map { tx -> TransactionStatus(tx.id, tx.timestamp, false) } }

    override fun saveOwner(safeAddress: Solidity.Address, safeOwner: AccountsRepository.SafeOwner) =
        tokenRepository.loadPaymentToken(safeAddress)
            .flatMapCompletable { accountsRepository.saveOwner(safeAddress, safeOwner, it) }
            .doOnComplete { pushServiceRepository.syncAuthentication(true) }
            .subscribeOn(Schedulers.io())

    override fun sign(safeAddress: Solidity.Address, data: ByteArray): Single<Signature> = accountsRepository.sign(safeAddress, data)

    private class SafeInfoRequest(
        val balance: EthRequest<Wei>,
        val threshold: EthRequest<String>,
        val owners: EthRequest<String>,
        val isOwner: EthRequest<String>,
        val modules: EthRequest<String>,
        val version: EthRequest<String>
    ) : BulkRequest(balance, threshold, owners, isOwner, modules, version)

    private class CheckSafeRequest(
        val masterCopy: EthRequest<String>,
        val threshold: EthRequest<String>
    ) : BulkRequest(masterCopy, threshold)

    companion object {
        private val SUPPORTED_SAFE_MASTER_COPIES = BuildConfig.SUPPORTED_SAFE_MASTER_COPY_ADDRESSES.split(",").map { it.asEthereumAddress()!! }
        private val NO_EXTENSION_THRESHOLD = BigInteger.ONE

        private val MASTER_COPY_ADDRESS = BuildConfig.CURRENT_SAFE_MASTER_COPY_ADDRESS.asEthereumAddress()!!
        private val PROXY_FACTORY_ADDRESS = BuildConfig.PROXY_FACTORY_ADDRESS.asEthereumAddress()!!
        private val FUNDER_ADDRESS = BuildConfig.SAFE_CREATION_FUNDER.asEthereumAddress()!!
        private val TX_ORIGIN_ADDRESS = "0x0".asEthereumAddress()!!
        private const val PROXY_CODE =
            "0x608060405234801561001057600080fd5b506040516020806101a88339810180604052602081101561003057600080fd5b8101908080519060200190929190505050600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614156100c7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004018080602001828103825260248152602001806101846024913960400191505060405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050606e806101166000396000f3fe608060405273ffffffffffffffffffffffffffffffffffffffff600054163660008037600080366000845af43d6000803e6000811415603d573d6000fd5b3d6000f3fea165627a7a723058201e7d648b83cfac072cbccefc2ffc62a6999d4a050ee87a721942de1da9670db80029496e76616c6964206d617374657220636f707920616464726573732070726f7669646564"
    }
}
