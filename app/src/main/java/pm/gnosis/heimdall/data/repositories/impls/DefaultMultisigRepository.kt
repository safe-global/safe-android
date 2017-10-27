package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.GnosisSafe.GetOwners
import pm.gnosis.heimdall.GnosisSafe.Required
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.models.MultisigWalletDb
import pm.gnosis.heimdall.data.db.models.fromDb
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.BulkRequest.SubRequest
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.models.JsonRpcRequest
import pm.gnosis.heimdall.data.remote.models.TransactionCallParams
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.heimdall.data.repositories.models.MultisigWalletInfo
import pm.gnosis.models.Wei
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMultisigRepository @Inject constructor(
        gnosisAuthenticatorDb: GnosisAuthenticatorDb,
        private val ethereumJsonRpcRepository: EthereumJsonRpcRepository
) : MultisigRepository {

    private val multisigWalletDao = gnosisAuthenticatorDb.multisigWalletDao()

    override fun observeMultisigWallets() =
            multisigWalletDao.observeMultisigWallets()
                    .map { it.map { (address, name) -> MultisigWallet(address, name) } }
                    .subscribeOn(Schedulers.io())!!

    override fun observeMultisigWallet(address: BigInteger): Flowable<MultisigWallet> =
            multisigWalletDao.observeMultisigWallet(address)
                    .subscribeOn(Schedulers.io())
                    .map { it.fromDb() }

    override fun addMultisigWallet(address: BigInteger, name: String?) =
            Completable.fromCallable {
                val multisigWallet = MultisigWalletDb(address, name)
                multisigWalletDao.insertMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())!!

    override fun removeMultisigWallet(address: BigInteger) =
            Completable.fromCallable {
                multisigWalletDao.removeMultisigWallet(address)
            }.subscribeOn(Schedulers.io())!!

    override fun updateMultisigWalletName(address: BigInteger, newName: String) =
            Completable.fromCallable {
                val multisigWallet = MultisigWalletDb(address, newName)
                multisigWalletDao.updateMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())!!

    override fun loadMultisigWalletInfo(address: BigInteger): Observable<MultisigWalletInfo> {
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
                    MultisigWalletInfo(addressString,
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