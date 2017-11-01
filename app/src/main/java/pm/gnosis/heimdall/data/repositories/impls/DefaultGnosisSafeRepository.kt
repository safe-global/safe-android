package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe.GetOwners
import pm.gnosis.heimdall.GnosisSafe.Required
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
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
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
                    .map { it.map { (address, name) -> Safe(address, name) } }
                    .subscribeOn(Schedulers.io())!!

    override fun observeSafe(address: BigInteger): Flowable<Safe> =
            safeDao.observeSafe(address)
                    .subscribeOn(Schedulers.io())
                    .map { it.fromDb() }

    override fun add(address: BigInteger, name: String?) =
            Completable.fromCallable {
                val multisigWallet = GnosisSafeDb(address, name)
                safeDao.insertSafe(multisigWallet)
            }.subscribeOn(Schedulers.io())!!

    override fun remove(address: BigInteger) =
            Completable.fromCallable {
                safeDao.removeSafe(address)
            }.subscribeOn(Schedulers.io())!!

    override fun updateName(address: BigInteger, newName: String) =
            Completable.fromCallable {
                val multisigWallet = GnosisSafeDb(address, newName)
                safeDao.updateSafe(multisigWallet)
            }.subscribeOn(Schedulers.io())!!

    override fun loadInfo(address: BigInteger): Observable<SafeInfo> {
        val addressString = address.asEthereumAddressString()
        val request = WalletInfoRequest(
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

    private class WalletInfoRequest(
            val balance: SubRequest<Wei>,
            val requiredConfirmations: SubRequest<Required.Return>,
            val owners: SubRequest<GetOwners.Return>
    ) : BulkRequest(balance, requiredConfirmations, owners)
}