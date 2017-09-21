package pm.gnosis.heimdall.ui.tokens

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.heimdall.data.db.ERC20Token
import pm.gnosis.heimdall.data.db.GnosisAuthenticatorDb
import pm.gnosis.heimdall.data.remote.EthereumJsonRpcRepository
import pm.gnosis.heimdall.common.di.ForView
import pm.gnosis.utils.hexAsBigInteger
import javax.inject.Inject

@ForView
class TokensPresenter @Inject constructor(private val gnosisAuthenticatorDb: GnosisAuthenticatorDb,
                                          private val ethereumJsonRpcRepository: EthereumJsonRpcRepository) {
    fun observeTokens(): Flowable<List<ERC20Token>> =
            gnosisAuthenticatorDb.erc20TokenDao().observeTokens().subscribeOn(Schedulers.io())

    fun observeTokenInfo(token: ERC20Token) =
            token.address?.let { ethereumJsonRpcRepository.getTokenInfo(it.hexAsBigInteger()) }

    fun addToken(address: String, name: String = "") = Completable.fromCallable {
        val token = ERC20Token()
        token.address = address
        token.name = name
        gnosisAuthenticatorDb.erc20TokenDao().insertERC20Token(token)
    }.subscribeOn(Schedulers.io())

    fun removeToken(token: ERC20Token) = Completable.fromCallable {
        gnosisAuthenticatorDb.erc20TokenDao().deleteToken(token)
    }.subscribeOn(Schedulers.io())
}
