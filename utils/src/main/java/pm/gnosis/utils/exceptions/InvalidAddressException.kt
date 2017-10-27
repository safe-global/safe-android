package pm.gnosis.utils.exceptions

import pm.gnosis.utils.asEthereumAddressStringOrNull
import java.math.BigInteger

class InvalidAddressException : Exception {
    constructor() : super("Invalid address")
    constructor(address: BigInteger? = null) : super("Invalid address: ${address?.asEthereumAddressStringOrNull()}")
    constructor(address: String? = null) : super("Invalid address: $address")
}
