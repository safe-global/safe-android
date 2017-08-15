package pm.gnosis.android.app.authenticator.data.exceptions

class AddressInvalidException(val address: String? = null) : Exception("Address invalid: $address")
