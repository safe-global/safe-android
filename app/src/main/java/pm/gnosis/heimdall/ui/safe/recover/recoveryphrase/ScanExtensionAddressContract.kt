package pm.gnosis.heimdall.ui.safe.recover.recoveryphrase

import androidx.lifecycle.ViewModel
import io.reactivex.Single
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.Result
import pm.gnosis.utils.asEthereumAddressString

abstract class ScanExtensionAddressContract : ViewModel() {
    abstract fun setup(safeAddress: Solidity.Address)
    abstract fun getSafeAddress(): Solidity.Address
    abstract fun validatePayload(payload: String): Single<Result<Solidity.Address>>

    class InvalidPayloadFormatException : Exception()
    class InvalidSafeAddressException(expectedSafeAddress: Solidity.Address, payloadSafeAddress: Solidity.Address) :
        Exception("Scanned safe address (${payloadSafeAddress.asEthereumAddressString()}) does not match the expected address (${expectedSafeAddress.asEthereumAddressString()})")

    class ExtensionIsNotOwnerException(extensionAddress: Solidity.Address) :
        Exception("${extensionAddress.asEthereumAddressString()} is not an owner of this safe")
}
