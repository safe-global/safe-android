package pm.gnosis.utils.exceptions

import java.math.BigInteger

class InvalidAddressException : Exception {
    constructor() : super("Invalid address")
    constructor(address: BigInteger? = null) : super("Invalid address: ${address?.toString(16)}")
    constructor(address: String? = null) : super("Invalid address: $address")
}
