package pm.gnosis.heimdall.data.repositories.impls

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit
import pm.gnosis.heimdall.MultiSigWalletWithDailyLimit.*
import pm.gnosis.heimdall.common.util.ERC20
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.db.model.MultisigWalletDb
import pm.gnosis.heimdall.data.model.JsonRpcRequest
import pm.gnosis.heimdall.data.model.TransactionCallParams
import pm.gnosis.heimdall.data.model.Wei
import pm.gnosis.heimdall.data.remote.BulkRequest
import pm.gnosis.heimdall.data.remote.BulkRequest.Call
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.data.remote.impl.SimpleEthereumJsonRpcRepository
import pm.gnosis.heimdall.data.repositories.MultisigRepository
import pm.gnosis.heimdall.data.repositories.model.ERC20Token
import pm.gnosis.heimdall.data.repositories.model.MultisigWallet
import pm.gnosis.heimdall.data.repositories.model.MultisigWalletInfo
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.toAlfaNumericAscii
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMultisigRepository @Inject constructor(
        gnosisAuthenticatorDb: GnosisAuthenticatorDb,
        val ethereumJsonRpcRepository: EthereumJsonRpcRepository
) : MultisigRepository {

    private val multisigWalletDao = gnosisAuthenticatorDb.multisigWalletDao()

    override fun observeMultisigWallets() =
            multisigWalletDao.observeMultisigWallets()
                    .map { it.map { (address, name) -> MultisigWallet(address, name) } }
                    .subscribeOn(Schedulers.io())!!

    override fun addMultisigWallet(address: String, name: String) =
            Completable.fromCallable {
                val multisigWallet = MultisigWalletDb(address, name)
                multisigWalletDao.insertMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())!!

    override fun removeMultisigWallet(address: String) =
            Completable.fromCallable {
                multisigWalletDao.removeMultisigWallet(address)
            }.subscribeOn(Schedulers.io())!!

    override fun updateMultisigWalletName(address: String, newName: String) =
            Completable.fromCallable {
                val multisigWallet = MultisigWalletDb(address, newName)
                multisigWalletDao.updateMultisigWallet(multisigWallet)
            }.subscribeOn(Schedulers.io())!!

    override fun loadMultisigWalletInfo(address: String): Observable<MultisigWalletInfo> {
        val request = WalletInfoRequest(
                Call(JsonRpcRequest(
                        id = 0,
                        method = EthereumJsonRpcRepository.FUNCTION_GET_BALANCE,
                        params = arrayListOf(address, EthereumJsonRpcRepository.DEFAULT_BLOCK_LATEST)),
                        { Wei(it.result.hexAsBigInteger()) }),
                Call(TransactionCallParams(to = address, data = Required.encode()).callRequest(1),
                        { Required.decode(it.result) }),
                Call(TransactionCallParams(to = address, data = GetOwners.encode()).callRequest(2),
                        { GetOwners.decode(it.result) })
        )
        return ethereumJsonRpcRepository.bulk(request)
                .map {
                    MultisigWalletInfo(address,
                            it.balance.value!!,
                            it.requiredConfirmations.value!!.param0.value.toLong(),
                            it.owners.value!!.param0.items.map { it.value.toString(16) })
                }
    }

    private class WalletInfoRequest(
            val balance: Call<Wei>,
            val requiredConfirmations: Call<Required.Return>,
            val owners: Call<GetOwners.Return>
    ) : BulkRequest(balance, requiredConfirmations, owners)
}