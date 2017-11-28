package pm.gnosis.utils.exceptions

import java.math.BigInteger

class InvalidTransactionHashException : Exception {
    constructor() : super("Invalid transaction hash")
    constructor(address: BigInteger? = null) : super("Invalid transaction hash: ${address?.toString(16)}")
    constructor(address: String? = null) : super("Invalid transaction hash: $address")
}