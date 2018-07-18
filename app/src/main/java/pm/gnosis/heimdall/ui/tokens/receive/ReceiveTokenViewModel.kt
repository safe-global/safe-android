package pm.gnosis.heimdall.ui.tokens.receive

import android.content.Context
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.utils.emitAndNext
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.QrCodeGenerator
import javax.inject.Inject

class ReceiveTokenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository,
    private var qrCodeGenerator: QrCodeGenerator
) : ReceiveTokenContract() {
    override fun observeSafeInfo(safeAddress: Solidity.Address): Observable<ViewUpdate> =
        Observable.merge(
            safeRepository.loadSafe(safeAddress).map {
                ViewUpdate.Info(it.displayName(context))
            }
                .onErrorReturn { ViewUpdate.Info(context.getString(R.string.default_safe_name)) }
                .toObservable(),
            Observable.fromCallable {
                safeAddress.asEthereumAddressChecksumString()
            }
                .subscribeOn(Schedulers.computation())
                .emitAndNext<String, ViewUpdate>(
                    emit = ViewUpdate::Address,
                    next = {
                        qrCodeGenerator.generateQrCode(it)
                            .map<ViewUpdate>(ViewUpdate::QrCode)
                            .toObservable()
                            // Ignore errors
                            .onErrorResumeNext(Observable.empty())
                    }
                )
        )

}
