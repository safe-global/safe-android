package pm.gnosis.heimdall.data.exceptions

import java.math.BigInteger

class InvalidAddressException(val address: BigInteger? = null) : Exception("Address invalid: $address")
