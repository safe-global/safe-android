package io.gnosis.safe

import android.content.Context
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.unstoppabledomains.exceptions.ns.NSExceptionCode
import com.unstoppabledomains.exceptions.ns.NamingServiceException
import io.gnosis.data.adapters.dataMoshi
import io.gnosis.data.repositories.EnsInvalidError
import io.gnosis.data.repositories.EnsResolutionError
import io.gnosis.data.repositories.EnsReverseRecordNotSetError
import io.gnosis.safe.helpers.Offline
import io.gnosis.safe.ui.safe.add.*
import io.gnosis.safe.ui.settings.owner.InvalidPrivateKey
import io.gnosis.safe.ui.settings.owner.InvalidSeedPhrase
import io.gnosis.safe.ui.settings.owner.KeyAlreadyImported
import io.gnosis.safe.ui.settings.owner.keystone.KeystoneSignFailed
import io.gnosis.safe.ui.transactions.details.MismatchingSafeTxHash
import pm.gnosis.utils.HttpCodes
import pm.gnosis.utils.exceptions.InvalidAddressException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

sealed class Error(
    val id: Int,
    open val httpCode: Int?,
    @StringRes
    val reason: Int,
    @StringRes
    val howToFix: Int,
    val trackingRequired: Boolean = false

) {
    // Network-related errors
    object Error101 : Error(101, 101, R.string.error_network_no_internet_reason, R.string.error_network_no_internet_fix)
    object Error102 : Error(102, 102, R.string.error_network_ssl_reason, R.string.error_network_ssl_fix)
    object Error103 : Error(103, 103, R.string.error_network_timeout_reason, R.string.error_network_timeout_fix) {
        override fun message(context: Context): String {
            return "${context.getString(reason, 30)}. ${context.getString(howToFix)}. (${context.getString(
                R.string.error_id,
                httpCode ?: id
            )})"
        }
    }

    object Error104 : Error(104, 104, R.string.error_network_unknown_host_reason, R.string.error_network_unknown_host_fix)
    object Error400 : Error(400, 400, R.string.error_network_request_reason, R.string.error_network_request_fix)
    object Error401 : Error(401, 401, R.string.error_network_not_authorized_reason, R.string.error_network_not_authorized_fix)
    object Error403 : Error(403, 403, R.string.error_network_not_authorized_reason, R.string.error_network_not_authorized_fix)
    object Error404 : Error(404, 404, R.string.error_network_safe_not_found_reason, R.string.error_network_try_again_fix)
    object Error42200 : Error(42200, 42200, R.string.error_network_unexpected_reason, R.string.error_network_unexpected_fix, true)
    object Error42201 :
        Error(42201, 42201, R.string.error_network_address_format_invalid_reason, R.string.error_network_address_format_invalid_fix, true)

    object Error42250 : Error(42250, 42250, R.string.error_network_safe_info_not_found_reason, R.string.error_network_safe_info_not_found_fix, true)
    data class Error422xx(override val httpCode: Int? = null) :
        Error(422, httpCode, R.string.error_network_unexpected_reason, R.string.error_network_unexpected_fix, true)

    data class Error500(override val httpCode: Int? = null) :
        Error(500, httpCode, R.string.error_network_server_side_reason, R.string.error_network_server_side_fix) {
        override fun message(context: Context): String {
            return "${context.getString(reason, 30)}. ${context.getString(howToFix)}. (${context.getString(
                R.string.error_id,
                httpCode ?: id
            )})"
        }
    }

    // Common client errors

    object Error1101 : Error(1101, null, R.string.error_client_safe_duplicate_reason, R.string.error_client_safe_duplicate_fix)
    object Error1102 : Error(1102, null, R.string.error_client_address_invalid_reason, R.string.error_client_address_invalid_fix)
    object Error1103 : Error(1103, null, R.string.error_client_seed_phrase_reason, R.string.error_client_seed_phrase_fix)
    object Error1104 : Error(1104, null, R.string.error_client_tx_hash_validation_reason, R.string.error_client_tx_hash_validation_fix)

    object Error1105 :
        Error(1105, null, R.string.error_client_unsupported_implementation_copy_reason, R.string.error_client_unsupported_implementation_copy_fix)

    object Error1106 : Error(1106, null, R.string.error_client_ens_resolution_reason, R.string.error_client_ens_resolution_fix)
    object Error1107 : Error(1107, null, R.string.error_client_ens_reverse_record_reason, R.string.error_client_ens_reverse_record_fix)
    object Error1108 : Error(1108, null, R.string.error_client_ens_invalid_reason, R.string.error_client_ens_invalid_fix)
    object Error1109 : Error(1109, null, R.string.error_client_safe_not_found_reason, R.string.error_client_safe_not_found_fix)
    object Error1110 : Error(1110, null, R.string.error_client_safe_name_invalid_reason, R.string.error_client_safe_name_invalid_fix)
    object Error1111 : Error(1111, null, R.string.error_client_key_already_imported_reason, R.string.error_client_key_already_imported_fix)

    object Error1113 : Error(1112, 400, R.string.error_network_request_reason, R.string.error_network_request_fix)

    object Error1114: Error(1114, null, R.string.error_client_address_not_matching_network_reason, R.string.error_client_address_not_matching_network_fix)

    object Error2100: Error(2100, null, R.string.error_client_keystone_sign_reason, R.string.error_client_keystone_sign_fix)

    object Error6357 : Error(6357, null, R.string.error_client_UD_invalid_domain_reason, R.string.error_client_UD_invalid_domain_fix)
    object Error6358 : Error(6358, null, R.string.error_client_UD_name_not_registered_reason, R.string.error_client_UD_name_not_registered_fix)

    object Error6359 : Error(6359, null, R.string.error_client_UD_record_not_found_reason, R.string.error_client_UD_record_not_found_fix)
    object Error6360 : Error(6360, null, R.string.error_client_UD_domain_not_configured_reason, R.string.error_client_UD_domain_not_configured_fix)

    object Error6361 : Error(
        6361,
        null,
        R.string.error_client_UD_blockchain_provider_is_not_accessible_reason,
        R.string.error_client_UD_blockchain_provider_is_not_accessible_fix
    )

    object Error6362 : Error(6362, null, R.string.error_client_UD_currency_not_found_reason, R.string.error_client_UD_currency_not_found_fix)

    object ErrorUnknown : Error(-1, null, R.string.error_unknown_reason, R.string.error_unknown_fix) {
        override fun message(context: Context): String {
            return "${context.getString(reason)}. ${context.getString(howToFix)}"
        }
    };


    open fun message(context: Context): String {
        return "${context.getString(reason)}. ${context.getString(howToFix)}. (${context.getString(R.string.error_id, httpCode ?: id)})"
    }

    fun message(context: Context, @StringRes description: Int): String {
        return "${context.getString(description)}: ${message(context)}"
    }
}

fun Throwable.toError(): Error =
    when {
        // Common client errors
        this is UsedSafeAddress -> Error.Error1101
        this is InvalidAddressException -> Error.Error1102
        this is AddressPrefixMismatch -> Error.Error1114
        this is InvalidSeedPhrase -> Error.Error1103
        this is InvalidPrivateKey -> Error.Error1103
        this is KeyAlreadyImported -> Error.Error1111
        this is MismatchingSafeTxHash -> Error.Error1104
        this is SafeNotSupported -> Error.Error1105
        this is EnsResolutionError -> Error.Error1106
        this is EnsReverseRecordNotSetError -> Error.Error1107
        this is EnsInvalidError -> Error.Error1108
        this is SafeNotFound -> Error.Error1109
        this is InvalidName -> Error.Error1110
        this is KeystoneSignFailed -> Error.Error2100

        // Network-related errors
        this is HttpException -> {
            this.let {
                val errorBodyString = it.response()?.errorBody()?.string()
                when {
                    this.code() == HttpCodes.BAD_REQUEST -> return if (errorBodyString == "[\"Cloud messaging token is linked to another device\"]") {
                        Error.Error1113
                    } else {
                        Error.Error400
                    }
                    this.code() == HttpCodes.UNAUTHORIZED -> Error.Error401
                    this.code() == HttpCodes.FORBIDDEN -> Error.Error403
                    this.code() == HttpCodes.NOT_FOUND -> Error.Error404
                    this.code() == HttpCodes.SERVER_ERROR -> Error.Error500(500)
                    this.code() == 422 -> {

                        val serverError = errorBodyString?.let {
                            if (errorBodyString.isNotEmpty()) {
                                serverErrorAdapter.fromJson(errorBodyString)
                            } else {
                                null
                            }
                        }

                        when {
                            serverError?.code == 1 -> Error.Error42201
                            serverError?.code == 50 -> Error.Error42250
                            serverError?.code == null -> Error.Error42200
                            serverError.code in 2..9 -> Error.Error422xx("4220${serverError.code}".toInt())
                            serverError.code >= 10 -> Error.Error422xx("422${serverError.code}".toInt())
                            else -> Error.Error42200
                        }
                    }
                    this.code() in 300..599 -> Error.Error500(this.code())
                    else -> Error.ErrorUnknown
                }
            }
        }
        this is Offline -> Error.Error101
        this is SSLHandshakeException || this.cause is SSLHandshakeException -> Error.Error102
        this is SocketTimeoutException -> Error.Error103
        this is UnknownHostException || this is ConnectException -> Error.Error104
        this is NamingServiceException -> {
            this.let {
                when (this.getCode()) {
                    NSExceptionCode.UnregisteredDomain -> Error.Error6358
                    NSExceptionCode.UnsupportedDomain -> Error.Error6357
                    NSExceptionCode.RecordNotFound -> Error.Error6359
                    NSExceptionCode.BlockchainIsDown -> Error.Error6361
                    NSExceptionCode.UnspecifiedResolver -> Error.Error6360
                    NSExceptionCode.UnknownCurrency -> Error.Error6362
                    else -> Error.ErrorUnknown
                }
            }
        }
        else -> Error.ErrorUnknown
    }

fun errorSnackbar(view: View, text: CharSequence, duration: Int = 6000, action: Pair<String, (View) -> Unit>? = null) =
    Snackbar.make(view, text, duration).apply {
        action?.let { setAction(it.first, it.second) }
        show()
    }

@JsonClass(generateAdapter = true)
data class ServerError(
    @Json(name = "code") val code: Int,
    @Json(name = "message") val message: String?,
    @Json(name = "arguments") val arguments: List<String>?
)

private val serverErrorAdapter = dataMoshi.adapter(ServerError::class.java)
