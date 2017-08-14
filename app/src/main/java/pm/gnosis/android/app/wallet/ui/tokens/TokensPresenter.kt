package pm.gnosis.android.app.wallet.ui.tokens

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.android.app.wallet.data.db.ERC20Token
import pm.gnosis.android.app.wallet.data.db.GnosisAuthenticatorDb
import pm.gnosis.android.app.wallet.data.remote.InfuraRepository
import pm.gnosis.android.app.wallet.di.ForView
import pm.gnosis.android.app.wallet.util.hexAsBigInteger
import javax.inject.Inject

@ForView
class TokensPresenter @Inject constructor(private val gnosisAuthenticatorDb: GnosisAuthenticatorDb,
                                          private val infuraRepository: InfuraRepository) {
    fun observeTokens(): Flowable<List<ERC20Token>> =
            gnosisAuthenticatorDb.erc20TokenDao().observeTokens().subscribeOn(Schedulers.io())

    fun observeTokenInfo(token: ERC20Token) =
            token.address?.let { infuraRepository.getTokenInfo(it.hexAsBigInteger()) }

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
