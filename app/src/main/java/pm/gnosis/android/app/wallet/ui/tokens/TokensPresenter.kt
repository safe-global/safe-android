package pm.gnosis.android.app.wallet.ui.tokens

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
}
