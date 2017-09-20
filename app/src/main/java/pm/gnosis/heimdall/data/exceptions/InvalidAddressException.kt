package pm.gnosis.heimdall.data.exceptions

class InvalidAddressException(val address: String? = null) : Exception("Address invalid: $address")
