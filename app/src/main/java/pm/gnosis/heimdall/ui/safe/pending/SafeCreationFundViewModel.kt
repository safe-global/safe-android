package pm.gnosis.heimdall.ui.safe.pending

import android.content.Context
import io.reactivex.Observable
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SafeCreationFundViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository
) : SafeCreationFundContract() {

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context).build()

    private lateinit var safeAddress: Solidity.Address
    private var pendingSafe: PendingSafe? = null

    override fun setup(safeAddress: String) {
        this.safeAddress = safeAddress.asEthereumAddress()!!
    }

    override fun observeCreationInfo(): Observable<Result<CreationInfo>> =
        gnosisSafeRepository.observePendingSafe(safeAddress).toObservable()
            .doOnNext { this.pendingSafe = it }
            .emitAndNext(
                emit = { DataResult(CreationInfo(it.address.asEthereumAddressChecksumString(), null, it.paymentAmount)) },
                next = { safe ->
                    tokenRepository.loadToken(safe.paymentToken)
                        .onErrorResumeNext { errorHandler.single(it) }
                        .map { CreationInfo(safe.address.asEthereumAddressChecksumString(), it, safe.paymentAmount) }
                        .toObservable()
                        .mapToResult()
                }
            )

    override fun observeHasEnoughDeployBalance(): Observable<Unit> =
        gnosisSafeRepository.observePendingSafe(safeAddress).toObservable()
            .switchMap { pendingSafe ->
                // Create a fake token since only the address is necessary to load the balance
                requestBalance(pendingSafe.address, ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = ""))
                    .map { if (it < pendingSafe.paymentAmount) throw NotEnoughFundsException() }
                    .retryWhen { errors ->
                        errors.flatMap {
                            if (it is NotEnoughFundsException) Observable.just(it).delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS)
                            else Observable.error(it)
                        }
                    }
                    .flatMap { gnosisSafeRepository.updatePendingSafe(pendingSafe.copy(isFunded = true)).andThen(Observable.just(Unit)) }
            }

    private fun requestBalance(address: Solidity.Address, paymentToken: ERC20Token) =
        tokenRepository.loadTokenBalances(address, listOf(paymentToken)).map { it.first().second ?: BigInteger.ZERO }

    companion object {
        private const val BALANCE_REQUEST_INTERVAL_SECONDS = 10L
    }

    class NotEnoughFundsException : IllegalStateException()
}
