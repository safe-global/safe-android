package pm.gnosis.android.app.wallet.data.exceptions

class AddressInvalidException(val address: String? = null) : Exception("Address invalid: $address")
