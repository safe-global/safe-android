package io.gnosis.safe

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.unstoppabledomains.exceptions.ns.NSExceptionCode
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import io.gnosis.data.repositories.EnsInvalidError
import io.gnosis.data.repositories.EnsResolutionError
import io.gnosis.data.repositories.EnsReverseRecordNotSetError
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.safe.add.InvalidName
import io.gnosis.safe.ui.safe.add.SafeNotFound
import io.gnosis.safe.ui.safe.add.SafeNotSupported
import io.gnosis.safe.ui.safe.add.UsedSafeAddress
import io.gnosis.safe.ui.settings.owner.InvalidSeedPhrase
import io.gnosis.safe.ui.transactions.details.MismatchingSafeTxHash
import pm.gnosis.utils.HttpCodes
import pm.gnosis.utils.exceptions.InvalidAddressException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

enum class Error(
    val id: Int,
    @StringRes
    val reason: Int,
    @StringRes
    val howToFix: Int,
    val trackingRequired: Boolean = false

) {
    // Network-related errors
    ERROR_101(101, R.string.error_network_no_internet_reason, R.string.error_network_no_internet_fix),
    ERROR_102(102, R.string.error_network_ssl_reason, R.string.error_network_ssl_fix),
    ERROR_103(103, R.string.error_network_timeout_reason, R.string.error_network_timeout_fix) {
        override fun message(context: Context): String {
            return "${context.getString(reason, 30)}. ${context.getString(howToFix)}. (${context.getString(R.string.error_id, id)})"
        }
    },
    ERROR_104(104, R.string.error_network_unknown_host_reason, R.string.error_network_unknown_host_fix),
    ERROR_400(400, R.string.error_network_request_reason, R.string.error_network_request_fix),
    ERROR_401(401, R.string.error_network_not_authorized_reason, R.string.error_network_not_authorized_fix),
    ERROR_403(403, R.string.error_network_not_authorized_reason, R.string.error_network_not_authorized_fix),
    ERROR_500(500, R.string.error_network_server_side_reason, R.string.error_network_server_side_fix),

    // Common client errors
    ERROR_1101(1101, R.string.error_client_safe_duplicate_reason, R.string.error_client_safe_duplicate_fix),
    ERROR_1102(1102, R.string.error_client_address_invalid_reason, R.string.error_client_address_invalid_fix),
    ERROR_1103(1103, R.string.error_client_seed_phrase_reason, R.string.error_client_seed_phrase_fix),
    ERROR_1104(1104, R.string.error_client_tx_hash_validation_reason, R.string.error_client_tx_hash_validation_fix),
    ERROR_1105(1105, R.string.error_client_unsupported_implementation_copy_reason, R.string.error_client_unsupported_implementation_copy_fix),
    ERROR_1106(1106, R.string.error_client_ens_resolution_reason, R.string.error_client_ens_resolution_fix),
    ERROR_1107(1107, R.string.error_client_ens_reverse_record_reason, R.string.error_client_ens_reverse_record_fix),
    ERROR_1108(1108, R.string.error_client_ens_invalid_reason, R.string.error_client_ens_invalid_fix),
    ERROR_1109(1109, R.string.error_client_safe_not_found_reason, R.string.error_client_safe_not_found_fix),
    ERROR_1110(1110, R.string.error_client_safe_name_invalid_reason, R.string.error_client_safe_name_invalid_fix),

    ERROR_UD_UNSUPPORTED_DOMAIN(6357, R.string.error_client_UD_invalid_domain_reason, R.string.error_client_UD_invalid_domain_fix),
    ERROR_UD_UNREGISTERED(6358, R.string.error_client_UD_name_not_registered_reason, R.string.error_client_UD_name_not_registered_fix),
    ERROR_UD_RECORD_NOT_FOUND(6359, R.string.error_client_UD_record_not_found_reason, R.string.error_client_UD_record_not_found_fix),
    ERROR_UD_UNSPECIFIED_RESOLVER(6360, R.string.error_client_UD_domain_not_configured_reason, R.string.error_client_UD_domain_not_configured_fix),
    ERROR_UD_BLOCKHAIN_DOWN(6361, R.string.error_client_UD_blockchain_provider_is_not_accessible_reason, R.string.error_client_UD_blockchain_provider_is_not_accessible_fix),
    ERROR_UD_UNKNOWN_CURRENCY(6357, R.string.error_client_UD_currency_not_found_reason, R.string.error_client_UD_currency_not_found_fix),


    ERROR_UNKNOWN(-1, R.string.error_unknown_reason, R.string.error_unknown_fix) {
        override fun message(context: Context): String {
            return "${context.getString(reason)}. ${context.getString(howToFix)}"
        }
    };


    open fun message(context: Context): String {
        return "${context.getString(reason)}. ${context.getString(howToFix)}. (${context.getString(R.string.error_id, id)})"
    }

    fun message(context: Context, @StringRes description: Int): String {
        return "${context.getString(description)}: ${message(context)}"
    }
}

fun Throwable.toError(): Error =
    when {
        // Common client errors
        this is UsedSafeAddress -> Error.ERROR_1101
        this is InvalidAddressException -> Error.ERROR_1102
        this is InvalidSeedPhrase -> Error.ERROR_1103
        this is MismatchingSafeTxHash -> Error.ERROR_1104
        this is SafeNotSupported -> Error.ERROR_1105
        this is EnsResolutionError -> Error.ERROR_1106
        this is EnsReverseRecordNotSetError -> Error.ERROR_1107
        this is EnsInvalidError -> Error.ERROR_1108
        this is SafeNotFound -> Error.ERROR_1109
        this is InvalidName -> Error.ERROR_1110

        // Network-related errors
        this is HttpException -> {
            this.let {
                when (this.code()) {
                    HttpCodes.BAD_REQUEST -> Error.ERROR_400
                    HttpCodes.UNAUTHORIZED -> Error.ERROR_401
                    HttpCodes.FORBIDDEN -> Error.ERROR_403
                    HttpCodes.SERVER_ERROR -> Error.ERROR_500
                    else -> Error.ERROR_UNKNOWN
                }
            }
        }
        this is Offline -> Error.ERROR_101
        this is SSLHandshakeException || this.cause is SSLHandshakeException -> Error.ERROR_102
        this is SocketTimeoutException -> Error.ERROR_103
        this is UnknownHostException || this is ConnectException -> Error.ERROR_104
        this is NamingServiceException -> {
            this.let {
                when (this.getCode()) {
                    NSExceptionCode.UnregisteredDomain -> Error.ERROR_UD_UNREGISTERED
                    NSExceptionCode.UnsupportedDomain -> Error.ERROR_UD_UNSUPPORTED_DOMAIN
                    NSExceptionCode.RecordNotFound -> Error.ERROR_UD_RECORD_NOT_FOUND
                    NSExceptionCode.BlockchainIsDown -> Error.ERROR_UD_BLOCKHAIN_DOWN
                    NSExceptionCode.UnspecifiedResolver -> Error.ERROR_UD_UNSPECIFIED_RESOLVER
                    NSExceptionCode.UnknownCurrency -> Error.ERROR_UD_UNKNOWN_CURRENCY
                    else -> Error.ERROR_UNKNOWN
                }
            }
        }

        else -> Error.ERROR_UNKNOWN
    }

fun errorSnackbar(view: View, text: CharSequence, duration: Int = 6000, action: Pair<String, (View) -> Unit>? = null) =
    Snackbar.make(view, text, duration).apply {
        action?.let { setAction(it.first, it.second) }
        show()
    }
