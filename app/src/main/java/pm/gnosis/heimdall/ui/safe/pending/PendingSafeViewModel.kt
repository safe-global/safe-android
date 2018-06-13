package pm.gnosis.heimdall.ui.safe.pending

import io.reactivex.Observable
import pm.gnosis.ethereum.EthBalance
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.data.repositories.models.PendingSafe
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PendingSafeViewModel @Inject constructor(
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val ethereumRepository: EthereumRepository
) : PendingSafeContract() {
    private lateinit var transactionHash: BigInteger
    private var pendingSafe: PendingSafe? = null

    override fun setup(transactionHash: String) {
        this.transactionHash = transactionHash.hexAsBigInteger()
    }

    override fun observePendingSafe(): Observable<PendingSafe> =
        gnosisSafeRepository.observePendingSafe(transactionHash).toObservable()
            .doOnNext { this.pendingSafe = it }

    override fun observeHasEnoughDeployBalance(): Observable<Unit> =
        gnosisSafeRepository.observePendingSafe(transactionHash).toObservable()
            .switchMap { pendingSafe ->
                requestBalance(pendingSafe.address)
                    .map { if (it < pendingSafe.payment) throw NotEnoughFundsException() }
                    .retryWhen { errors ->
                        errors.flatMap {
                            if (it is NotEnoughFundsException) Observable.just(it).delay(BALANCE_REQUEST_INTERVAL_SECONDS, TimeUnit.SECONDS)
                            else Observable.error(it)
                        }
                    }
                    .flatMap { gnosisSafeRepository.updatePendingSafe(pendingSafe.copy(isFunded = true)).andThen(Observable.just(Unit)) }
            }

    override fun getTransactionHash() = pendingSafe?.hash

    private fun requestBalance(address: Solidity.Address) =
        ethereumRepository.request(EthBalance(address)).map { it.checkedResult("Could not retrieve balance of ${address.asEthereumAddressString()}") }

    companion object {
        private const val BALANCE_REQUEST_INTERVAL_SECONDS = 10L
    }

    class NotEnoughFundsException : IllegalStateException()
}
