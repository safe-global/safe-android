package pm.gnosis.heimdall.utils

import android.content.Intent
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

private const val EXTRA_AUTHENTICATOR_TYPE = "extra.int.authenticator_type"
private const val EXTRA_AUTHENTICATOR_ADDRESS = "extra.string.authenticator_address"
private const val EXTRA_SAFE_OWNER = "extra.parcelable.safe_owner"

data class AuthenticatorInfo(
    val type: Type,
    val address: Solidity.Address,
    val safeOwner: AccountsRepository.SafeOwner
) {
    enum class Type(val id: Int) {
        KEYCARD(0),
        EXTENSION(1)
    }
}

private fun Int.toAuthenticatorType() = when(this) {
    0 -> AuthenticatorInfo.Type.KEYCARD
    1 -> AuthenticatorInfo.Type.EXTENSION
    else -> null
}

fun AuthenticatorInfo?.put(intent: Intent): Intent {
    this?.let { intent.putExtra(EXTRA_AUTHENTICATOR_TYPE, type.id) }
    intent.putExtra(EXTRA_AUTHENTICATOR_ADDRESS, this?.address?.asEthereumAddressString())
    intent.putExtra(EXTRA_SAFE_OWNER, this?.safeOwner)
    return intent
}

fun Intent.getAuthenticatorInfo(): AuthenticatorInfo? {
    val type = getIntExtra(EXTRA_AUTHENTICATOR_TYPE, -1).toAuthenticatorType() ?: return null
    val address = getStringExtra(EXTRA_AUTHENTICATOR_ADDRESS)?.let { it.asEthereumAddress()!! } ?: return null
    val safeOwner = getParcelableExtra<AccountsRepository.SafeOwner>(EXTRA_SAFE_OWNER) ?: return null
    return AuthenticatorInfo(type, address, safeOwner)
}
