package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe.GetOwners
import pm.gnosis.heimdall.GnosisSafe.Required
import pm.gnosis.heimdall.GnosisSafeWithDescriptions.GetDescriptionCount
import pm.gnosis.heimdall.GnosisSafeWithDescriptions.GetDescriptions
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.models.GnosisSafeDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.BulkRequest.SubRequest
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.JsonRpcRequest
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.Safe
import pm.gnosis.heimdall.data.repositories.models.SafeInfo
import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.toHexString
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultGnosisSafeRepository @Inject constructor(
        gnosisAuthenticatorDb: GnosisAuthenticatorDb,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository
) : GnosisSafeRepository {

    private val safeDao = gnosisAuthenticatorDb.gnosisSafeDao()

    override fun observeSafes() =
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
        val request = SafeInfoRequest(
                SubRequest(JsonRpcRequest(
                        id = 0,
                        method = EthereumJsonRpcRepository.FUNCTION_GET_BALANCE,
                        params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)),
                        { Wei(it.result.hexAsBigInteger()) }),
                SubRequest(TransactionCallParams(to = addressString, data = Required.encode()).callRequest(1),
                        { Required.decode(it.result) }),
                SubRequest(TransactionCallParams(to = addressString, data = GetOwners.encode()).callRequest(2),
                        { GetOwners.decode(it.result) })
        )
        return ethereumJsonRpcRepository.bulk(request)
                .map {
                    SafeInfo(addressString,
                            it.balance.value!!,
                            it.requiredConfirmations.value!!.param0.value.toLong(),
                            it.owners.value!!.param0.items.map { it.value.toString(16) })
                }
    }

    override fun loadDescriptionCount(address: BigInteger): Observable<Int> {
        val addressString = address.asEthereumAddressString()
        return ethereumJsonRpcRepository.call(TransactionCallParams(to = addressString, data = GetDescriptionCount.encode()))
                .map { GetDescriptionCount.decode(it).param0.value.toInt() }
    }

    override fun loadDescriptions(address: BigInteger, from: Int, to: Int): Observable<List<String>> {
        val addressString = address.asEthereumAddressString()
        val fromParam = Solidity.UInt256(BigInteger.valueOf(from.toLong()))
        val toParam = Solidity.UInt256(BigInteger.valueOf(to.toLong()))
        return ethereumJsonRpcRepository.call(TransactionCallParams(to = addressString, data = GetDescriptions.encode(fromParam, toParam)))
                .map {
                    GetDescriptions.decode(it)._descriptionhashes.items.map {
                        it.bytes.toHexString()
                    }
                }
    }

    private class SafeInfoRequest(
            val balance: SubRequest<Wei>,
            val requiredConfirmations: SubRequest<Required.Return>,
            val owners: SubRequest<GetOwners.Return>
    ) : BulkRequest(balance, requiredConfirmations, owners)
}