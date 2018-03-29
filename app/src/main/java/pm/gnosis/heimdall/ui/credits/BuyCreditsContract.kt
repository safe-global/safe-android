package pm.gnosis.heimdall.ui.credits

import android.app.Activity
import android.arch.lifecycle.ViewModel
import io.reactivex.Observable
import io.reactivex.Single
import pm.gnosis.svalinn.common.utils.Result


abstract class BuyCreditsContract : ViewModel() {
    abstract fun buyCredits(activity: Activity): Single<Boolean>
    abstract fun loadBalance(): Single<Long>
    abstract fun observeVoucherState(): Observable<VoucherState>
    abstract fun redeemPendingVoucher(): Observable<VoucherState>

    sealed class VoucherState {
        object Bought : VoucherState()
        object NoVoucher : VoucherState()
        object Redeeming : VoucherState()
        object AlreadyRedeemed : VoucherState()
        class Redeemed(val balance: Long) : VoucherState()
        class Error(val error: Throwable) : VoucherState()
    }
}
