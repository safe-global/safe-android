package pm.gnosis.heimdall.ui.credits

import android.app.Activity
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.heimdall.data.repositories.TxExecutorRepository
import pm.gnosis.svalinn.common.utils.DataResult
import pm.gnosis.svalinn.common.utils.ErrorResult
import pm.gnosis.svalinn.common.utils.Result
import javax.inject.Inject


class BuyCreditsViewModel @Inject constructor(
    private val txExecutorRepository: TxExecutorRepository
) : BuyCreditsContract() {

    override fun buyCredits(activity: Activity): Single<Boolean> =
        txExecutorRepository.buyCredits(activity)

    override fun loadBalance(): Single<Long> =
        txExecutorRepository.loadBalance()

    override fun redeemPendingVoucher(): Observable<VoucherState> =
        txExecutorRepository.observeVoucher().firstOrError()
            .flatMapObservable(::handleVoucher)

    override fun observeVoucherState(): Observable<VoucherState> =
        txExecutorRepository.observeVoucher()
            .flatMap(::handleVoucher)

    private fun handleVoucher(voucherResult: Result<String>) =
        when (voucherResult) {
            is DataResult -> redeemVoucher(voucherResult.data)
            is ErrorResult -> Observable.just(VoucherState.NoVoucher)
        }

    private fun redeemVoucher(voucher: String): Observable<VoucherState> =
        Observable.just<VoucherState>(VoucherState.Redeeming)
            .concatWith(
                txExecutorRepository.redeemVoucher(voucher)
                    .map<VoucherState> {
                        if (it < 0) VoucherState.AlreadyRedeemed
                        else VoucherState.Redeemed(it)
                    }
                    .onErrorReturn { VoucherState.Error(it) }
                    .toObservable()
            )
}
