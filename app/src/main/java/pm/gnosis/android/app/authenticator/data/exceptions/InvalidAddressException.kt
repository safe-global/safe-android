package pm.gnosis.android.app.authenticator.data.exceptions

class InvalidAddressException(val address: String? = null) : Exception("Address invalid: $address")
