package pm.gnosis.heimdall.ui.multisig.details

import android.content.Context
import android.graphics.Bitmap
import io.reactivex.Single
import pm.gnosis.heimdall.common.di.ApplicationContext
import pm.gnosis.heimdall.common.utils.QrCodeGenerator
import pm.gnosis.heimdall.common.utils.Result
import pm.gnosis.heimdall.common.utils.mapToResult
import pm.gnosis.heimdall.data.repositories.models.MultisigWallet
import pm.gnosis.heimdall.ui.exceptions.LocalizedException
import pm.gnosis.utils.exceptions.InvalidAddressException
import pm.gnosis.utils.isValidEthereumAddress
import java.math.BigInteger
import javax.inject.Inject

class MultisigDetailsViewModel @Inject constructor(
        @ApplicationContext private val context: Context,
        private val qrCodeGenerator: QrCodeGenerator
) : MultisigDetailsContract() {
    private lateinit var multisigWallet: MultisigWallet

    private val errorHandler = LocalizedException.networkErrorHandlerBuilder(context)
            .build()

    override fun setup(address: BigInteger, name: String?) {
        if (!address.isValidEthereumAddress()) throw InvalidAddressException(address)
        multisigWallet = MultisigWallet(address, name)
    }

    override fun getMultisigWallet() = multisigWallet

    override fun loadQrCode(contents: String): Single<Result<Bitmap>> =
            qrCodeGenerator.generateQrCode(contents)
                    .onErrorResumeNext { throwable: Throwable -> errorHandler.single(throwable) }
                    .mapToResult()
}
