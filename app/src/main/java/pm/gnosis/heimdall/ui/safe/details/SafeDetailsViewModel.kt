package pm.gnosis.heimdall.ui.safe.details

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Single
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.isValidEthereumAddress
import java.math.BigInteger
import javax.inject.Inject

class SafeDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository,
    private val qrCodeGenerator: QrCodeGenerator
) : SafeDetailsContract() {
    private lateinit var address: BigInteger

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
        .build()

    override fun setup(address: BigInteger, name: String?) {
        if (!address.isValidEthereumAddress()) throw InvalidAddressException(address)
        this.address = address
    }

    override fun observeSafe() = safeRepository.observeSafe(address)

    override fun loadQrCode(contents: String): Single<Result<Bitmap>> =
        qrCodeGenerator.generateQrCode(contents)
            .onErrorResumeNext { throwable: Throwable -> errorHandler.single(throwable) }
            .mapToResult()
}
