package pm.gnosis.heimdall.data.preferences

import android.content.Context
import android.content.SharedPreferences
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.data.repositories.models.ERC20Token.Companion.ETHER_TOKEN
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddress

class PreferencesToken(context: Context) {

    private val prefs: SharedPreferences

    init {
        prefs = context.getSharedPreferences(PREFERENCES_TOKEN, Context.MODE_PRIVATE)
    }

    var paymendToken: ERC20Token
        get() = prefs.run {
            getString(KEY_CURRENT_PAYMENT_TOKEN_ADDRESS, null)?.asEthereumAddress()?.let { tokenAddress ->
                ERC20Token(
                    tokenAddress,
                    getString(KEY_CURRENT_PAYMENT_TOKEN_NAME, null),
                    getString(KEY_CURRENT_PAYMENT_TOKEN_SYMBOL, null),
                    getInt(KEY_CURRENT_PAYMENT_TOKEN_DECIMALS, 0),
                    getString(KEY_CURRENT_PAYMENT_TOKEN_LOGO, null) ?: ""
                )

            }
        } ?: ETHER_TOKEN
        set(token) {
            prefs.edit {
                putString(KEY_CURRENT_PAYMENT_TOKEN_ADDRESS, token.address.asEthereumAddressChecksumString())
                putString(KEY_CURRENT_PAYMENT_TOKEN_NAME, token.name)
                putString(KEY_CURRENT_PAYMENT_TOKEN_SYMBOL, token.symbol)
                putInt(KEY_CURRENT_PAYMENT_TOKEN_DECIMALS, token.decimals)
                putString(KEY_CURRENT_PAYMENT_TOKEN_LOGO, token.logoUrl)
            }
        }

    companion object {
        private const val PREFERENCES_TOKEN = "TokenRepoPreferences"
        private const val KEY_CURRENT_PAYMENT_TOKEN_ADDRESS = "default_token_repo.string.current_payment_token_address"
        private const val KEY_CURRENT_PAYMENT_TOKEN_NAME = "default_token_repo.string.current_payment_token_name"
        private const val KEY_CURRENT_PAYMENT_TOKEN_SYMBOL = "default_token_repo.string.current_payment_token_symbol"
        private const val KEY_CURRENT_PAYMENT_TOKEN_DECIMALS = "default_token_repo.int.current_payment_token_decimals"
        private const val KEY_CURRENT_PAYMENT_TOKEN_LOGO = "default_token_repo.string.current_payment_token_logo"

    }
}