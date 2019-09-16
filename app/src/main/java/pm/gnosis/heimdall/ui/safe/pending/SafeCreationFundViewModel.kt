package pm.gnosis.heimdall.ui.safe.pending

import android.content.Context
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.TokenRepository
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20TokenWithBalance
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.heimdall.utils.SafeContractUtils
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.asEthereumAddress
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SafeCreationFundViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val tokenRepository: TokenRepository,
    private val ethereumRepository: EthereumRepository,
    private var qrCodeGenerator: QrCodeGenerator
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
                emit = { DataResult(CreationInfo(it.address.asEthereumAddressChecksumString(), null, it.paymentAmount, null)) },
                next = { safe ->
                    tokenRepository.loadToken(safe.paymentToken)
                        .onErrorResumeNext { errorHandler.single(it) }
                        .toObservable()
                        .flatMap { token ->
                            qrCodeGenerator.generateQrCode(safe.address.asEthereumAddressChecksumString())
                                .toObservable()
                                .subscribeOn(Schedulers.computation())
                                .map {
                                    token to it
                                }
                        }
                        .flatMap { (token, qrCode) ->
                            ethereumRepository.getBalance(safe.address)
                                .map {
                                    Triple(token, it, qrCode)
                                }
                        }
                        .map { (token, balance, qrCode) ->
                            CreationInfo(safe.address.asEthereumAddressChecksumString(), ERC20TokenWithBalance(token, balance.toEther().toBigInteger()), safe.paymentAmount, qrCode)
                        }
                        .mapToResult()
                }
            )


    override fun observeHasEnoughDeployBalance(): Observable<Unit> =
        gnosisSafeRepository.observePendingSafe(safeAddress).toObservable()
            .switchMap { pendingSafe ->
                gnosisSafeRepository.checkSafe(safeAddress)
                    .retryWhen { errors -> errors.flatMap { Observable.just(it).delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS) } }
                    .switchMapCompletable { (masterCopy) ->
                        if (SafeContractUtils.isSupported(masterCopy))
                            gnosisSafeRepository.pendingSafeToDeployedSafe(pendingSafe)
                        else
                            checkFunding(pendingSafe)
                    }
                    .andThen(Observable.just(Unit))
            }

    private fun checkFunding(pendingSafe: PendingSafe) =
        // Create a fake token since only the address is necessary to load the balance
        tokenRepository.loadTokenBalances(pendingSafe.address, listOf(ERC20Token(pendingSafe.paymentToken, decimals = 0, name = "", symbol = "")))
            .map { it.first().second ?: BigInteger.ZERO }
            .map { if (it < pendingSafe.paymentAmount) throw NotEnoughFundsException() }
            .retryWhen { errors -> errors.flatMap { Observable.just(it).delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS) } }
            .flatMapCompletable { gnosisSafeRepository.updatePendingSafe(pendingSafe.copy(isFunded = true)) }

    companion object {
        private const val BALANCE_REQUEST_INTERVAL_SECONDS = 10L
    }

    class NotEnoughFundsException : IllegalStateException()
}
