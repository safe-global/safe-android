package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import android.content.Context
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.models.AddressSignedPayload
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.ui.exceptions.SimpleLocalizedException
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.svalinn.common.utils.mapToResult
import pm.gnosis.utils.nullOnThrow
import javax.inject.Inject

class ScanExtensionAddressViewModel @Inject constructor(
    private val cryptoHelper: CryptoHelper,
    @ApplicationContext private val context: Context,
    private val safeRepository: GnosisSafeRepository,
    moshi: Moshi
) : ScanExtensionAddressContract() {
    private lateinit var safeAddress: Solidity.Address

    private val addressSignedPayloadAdapter = moshi.adapter(AddressSignedPayload::class.java)

    private val errorHandler = SimpleLocalizedException.networkErrorHandlerBuilder(context)
        .add({ it is InvalidPayloadFormatException }, R.string.scan_extension_invalid_format)
        .add({ it is InvalidSafeAddressException }, R.string.scan_extension_invalid_signature)
        .add({ it is ExtensionIsNotOwnerException }, R.string.scan_extension_not_owner)
        .build()


    override fun setup(safeAddress: Solidity.Address) {
        this.safeAddress = safeAddress
    }

    override fun getSafeAddress() = safeAddress

    override fun validatePayload(payload: String): Single<Result<Solidity.Address>> =
        Single.fromCallable {
            val parsedPayload = nullOnThrow { addressSignedPayloadAdapter.fromJson(payload) } ?: throw InvalidPayloadFormatException()
            // Check if the safe address corresponds to the the address provided in the payload
            if (safeAddress != parsedPayload.address) throw InvalidSafeAddressException(safeAddress, parsedPayload.address)
            val expectedData = Sha3Utils.keccak("$SIGNATURE_PREFIX${parsedPayload.address.asEthereumAddressChecksumString()}".toByteArray())
            parsedPayload to expectedData
        }
            .subscribeOn(Schedulers.computation())
            .map { (payload, expectedData) -> cryptoHelper.recover(expectedData, payload.signature.toSignature()) }
            .flatMap { recoveredExtensionAddress -> isBrowserExtensionOwner(recoveredExtensionAddress) }
            .onErrorResumeNext { errorHandler.single(it) }
            .mapToResult()

    private fun isBrowserExtensionOwner(extensionAddress: Solidity.Address): Single<Solidity.Address> =
        safeRepository.loadInfo(safeAddress).firstOrError()
            .map { safeInfo ->
                if (safeInfo.owners.contains(extensionAddress)) extensionAddress
                else throw ExtensionIsNotOwnerException(extensionAddress)
            }

    companion object {
        private const val SIGNATURE_PREFIX = "GNO"
    }
}
