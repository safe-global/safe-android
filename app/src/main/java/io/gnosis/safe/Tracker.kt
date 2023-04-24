package io.gnosis.safe

import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.gnosis.safe.Tracker.ParamValues.KEY_IMPORT_TYPE_KEY
import io.gnosis.safe.Tracker.ParamValues.KEY_IMPORT_TYPE_SEED
import java.math.BigInteger
import javax.inject.Singleton

@Singleton
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
class Tracker internal constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val firebaseCrashlytics: FirebaseCrashlytics
) {

    fun setNumSafes(numSafes: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_SAFES, numSafes.toString())
    }

    fun setNumKeysGenerated(numKeysGenerated: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_KEYS_GENERATED, numKeysGenerated.toString())
    }

    fun setNumKeysImported(numKeysImported: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_KEYS_IMPORTED, numKeysImported.toString())
    }

    fun setNumKeysLedger(numKeysLedger: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_KEYS_LEDGER, numKeysLedger.toString())
    }

    fun setNumKeysKeystone(numKeysKeystone: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_KEYS_KEYSTONE, numKeysKeystone.toString())
    }

    fun setPushInfo(enabled: Boolean) {
        firebaseAnalytics.setUserProperty(Param.PUSH_INFO, if (enabled) ParamValues.PUSH_ENABLED else ParamValues.PUSH_DISABLED)
    }

    fun setPasscodeIsSet(usePasscode: Boolean) {
        firebaseAnalytics.setUserProperty(Param.PASSCODE_IS_SET, usePasscode.toString())
    }

    fun logScreen(screenId: ScreenId, chainId: BigInteger? = null) {
        logEvent(
            screenId.value,
            chainId?.let {
                mapOf(
                    Param.CHAIN_ID to it.toString()
                )
            }
        )
    }

    fun logSafeAdded(chainId: BigInteger) {
        logEvent(
            Event.SAFE_ADDED,
            mapOf(
                Param.CHAIN_ID to chainId
            )
        )
    }

    fun logSafeRemoved(chainId: BigInteger) {
        logEvent(
            Event.SAFE_REMOVED,
            mapOf(
                Param.CHAIN_ID to chainId
            )
        )
    }

    fun logTransferReceiveClicked() {
        logEvent(Event.TRANSFER_RECEIVE_CLICKED, null)
    }

    fun logTransferSendClicked() {
        logEvent(Event.TRANSFER_SEND_CLICKED, null)
    }

    fun logTransferAddOwnerClicked() {
        logEvent(Event.TRANSFER_ADD_OWNER_CLICKED, null)
    }

    fun logKeyGenerated() {
        logEvent(Event.KEY_GENERATED, null)
    }

    fun logKeyImported(usingSeedPhrase: Boolean) {
        logEvent(
            Event.KEY_IMPORTED,
            mapOf(
                Param.KEY_IMPORT_TYPE to if (usingSeedPhrase) KEY_IMPORT_TYPE_SEED else KEY_IMPORT_TYPE_KEY
            )
        )
    }

    fun logLedgerKeyImported() {
        logEvent(Event.KEY_IMPORTED_LEDGER, null)
    }

    fun logKeystoneKeyImported() {
        logEvent(Event.KEY_IMPORTED_KEYSTONE, null)
    }

    fun logKeyDeleted() {
        logEvent(name = Event.KEY_DELETED, attrs = null)
    }

    fun logTransactionConfirmed(chainId: BigInteger) {
        logEvent(
            Event.TRANSACTION_CONFIRMED,
            mapOf(
                Param.CHAIN_ID to chainId
            )
        )
    }

    fun logTransactionRejected(chainId: BigInteger) {
        logEvent(
            Event.TRANSACTION_REJECTED,
            mapOf(
                Param.CHAIN_ID to chainId
            )
        )
    }

    fun logBannerPasscodeSkip() {
        logEvent(Event.BANNER_PASSCODE_SKIP, null)
    }

    fun logBannerPasscodeCreate() {
        logEvent(Event.BANNER_PASSCODE_CREATE, null)
    }

    fun logBannerOwnerSkip() {
        logEvent(Event.BANNER_OWNER_SKIP, null)
    }

    fun logBannerOwnerImport() {
        logEvent(Event.BANNER_OWNER_IMPORT, null)
    }

    fun logOnboardingOwnerSkipped() {
        logEvent(Event.ONBOARDING_OWNER_SKIPPED, null)
    }

    fun logOnboardingOwnerImport() {
        logEvent(Event.ONBOARDING_OWNER_IMPORT, null)
    }

    fun logPasscodeEnabled() {
        logEvent(Event.PASSCODE_ENABLED, null)
    }

    fun logPasscodeDisabled() {
        logEvent(Event.PASSCODE_DISABLED, null)
    }

    fun logPasscodeReset() {
        logEvent(Event.PASSCODE_RESET, null)
    }

    fun logPasscodeSkipped() {
        logEvent(Event.PASSCODE_SKIPPED, null)
    }

    fun logIntercomChatOpened() {
        logEvent(Event.INTERCOM_CHAT_OPENED, null)
    }

    fun logTxExecFieldsEdit(fields: List<TxExecField>) {
        logEvent(
            Event.TRANSACTION_EXEC_EDIT_FEE_FIELDS,
            mapOf(
                Param.TX_EXEC_FIELDS to fields.joinToString(separator = ",") { it.value }
            )
        )
    }

    private fun logEvent(name: String, attrs: Map<String, Any?>?) {
        try {
            val bundle = Bundle()
            attrs?.let {
                for ((key, value) in attrs) {
                    when (value) {
                        is String -> bundle.putString(key, value)
                        is Long -> bundle.putLong(key, value)
                        is Int -> bundle.putInt(key, value)
                        is BigInteger -> bundle.putString(key, value.toString())
                        is Number -> bundle.putFloat(key, value.toFloat())
                    }
                }
            }
            firebaseAnalytics.logEvent(name, bundle)
        } catch (e: Exception) {
            logException(e)
        }
    }

    fun logException(exception: Throwable) {
        firebaseCrashlytics.recordException(exception)
    }

    object Event {
        val SAFE_ADDED = "user_safe_added"
        val SAFE_REMOVED = "user_safe_removed"
        val TRANSFER_RECEIVE_CLICKED = "user_select_receive_asset"
        val TRANSFER_SEND_CLICKED = "user_select_send_asset"
        val TRANSFER_ADD_OWNER_CLICKED = "user_select_add_owner_to_send_assets"
        val KEY_GENERATED = "user_key_generated"
        val KEY_IMPORTED = "user_key_imported"
        val KEY_IMPORTED_LEDGER = "user_ledger_nano_x_key_imported"
        val KEY_IMPORTED_KEYSTONE = "user_keystone_key_imported"
        val KEY_DELETED = "user_key_deleted"
        val TRANSACTION_CONFIRMED = "user_transaction_confirmed"
        val TRANSACTION_CONFIRMED_LEDGER = "user_transaction_confirmed_ledger_nano_x"
        val TRANSACTION_REJECTED = "user_transaction_rejected"
        val TRANSACTION_REJECTED_LEDGER = "user_transaction_rejected_ledger_nano_x"
        val TRANSACTION_EXEC_EDIT_FEE_FIELDS = "user_edit_exec_tx_fee_fields"
        val TRANSACTION_EXEC_KEY_CHANGE = "user_select_exec_key_change"
        val TRANSACTION_EXEC_FAILED = "user_exec_tx_failed"
        val TRANSACTIONS_EXEC_SUBMITTED = "user_transaction_exec_submitted"
        val BANNER_PASSCODE_SKIP = "user_banner_passcode_skip"
        val BANNER_PASSCODE_CREATE = "user_banner_passcode_create"
        val BANNER_OWNER_SKIP = "user_banner_owner_skip"
        val BANNER_OWNER_IMPORT = "user_banner_owner_import"
        val ONBOARDING_OWNER_SKIPPED = "user_onboarding_owner_skip"
        val ONBOARDING_OWNER_IMPORT = "user_onboarding_owner_import"
        val PASSCODE_ENABLED = "user_passcode_enabled"
        val PASSCODE_DISABLED = "user_passcode_disabled"
        val PASSCODE_RESET = "user_passcode_reset"
        val PASSCODE_SKIPPED = "user_passcode_skipped"
        val INTERCOM_CHAT_OPENED = "user_settings_open_intercom"
    }

    object Param {
        val CHAIN_ID = "chain_id"
        val NUM_SAFES = "num_safes"
        val PUSH_INFO = "push_info"
        val NUM_KEYS_GENERATED = "num_keys_generated"
        val NUM_KEYS_IMPORTED = "num_keys_imported"
        val NUM_KEYS_LEDGER = "num_keys_ledger_nano_x"
        val NUM_KEYS_KEYSTONE = "num_keys_keystone"
        val KEY_IMPORT_TYPE = "import_type"
        val PASSCODE_IS_SET = "passcode_is_set"
        val TX_EXEC_FIELDS = "fields"
    }

    object ParamValues {
        val PUSH_ENABLED = "enabled"
        val PUSH_DISABLED = "disabled"
        val KEY_IMPORT_TYPE_SEED = "seed"
        val KEY_IMPORT_TYPE_KEY = "key"
    }

    companion object {

        @Volatile
        private var INSTANCE: Tracker? = null

        fun getInstance(context: Context): Tracker {
            val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
            val firebaseCrashlytics = FirebaseCrashlytics.getInstance()
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Tracker(firebaseAnalytics, firebaseCrashlytics).also { INSTANCE = it }
            }
        }
    }
}

enum class TxExecField(val value: String) {
    NONCE("nonce"),
    GAS_LIMIT("gasLimit"),
    GAS_PRICE("gasPrice"),
    MAX_FEE("maxFee"),
    MAX_PRIORITY_FEE("maxPriorityFee")
}

enum class ScreenId(val value: String) {
    LAUNCH("screen_launch"),
    LAUNCH_TERMS("screen_launch_terms"),
    CHAIN_LIST("screen_chain_list"),
    ASSETS_NO_SAFE("screen_assets_no_safe"),
    ASSETS_COINS("screen_assets_coins"),
    ASSETS_COINS_TRANSFER_SELECT("screen_select_asset"),
    ASSETS_COINS_TRANSFER("screen_asset_transfer"),
    ASSETS_COINS_TRANSFER_REVIEW("screen_review_asset_transfer"),
    ASSETS_COINS_TRANSFER_ADVANCED_PARAMS("screen_asset_transfer_advanced_params"),
    ASSETS_COINS_TRANSFER_SUCCESS("screen_asset_transfer_success"),
    ASSETS_COINS_TRANSFER_ADD_OWNER("screen_add_owner_to_send_funds"),
    ASSETS_COLLECTIBLES("screen_assets_collectibles"),
    ASSETS_COLLECTIBLES_DETAILS("screen_assets_collectibles_details"),
    OWNER_ADD_OPTIONS("screen_owner_options"),
    OWNER_INFO("screen_owner_info"),
    OWNER_GENERATE_INFO("screen_owner_generate_info"),
    OWNER_LEDGER_INFO("screen_owner_ledger_nano_x_info"),
    OWNER_KEYSTONE_INFO("screen_owner_keystone_info"),
    OWNER_ENTER_SEED("screen_owner_enter_seed"),
    OWNER_ENTER_NAME("screen_owner_enter_name"),
    OWNER_EDIT_NAME("screen_owner_edit_name"),
    OWNER_LIST("screen_owner_list"),
    OWNER_DETAILS("screen_owner_details"),
    OWNER_EXPORT_SEED("screen_owner_export_seed"),
    OWNER_EXPORT_KEY("screen_owner_export_key"),
    SAFE_RECEIVE("screen_safe_receive"),
    SAFE_SELECT("screen_safe_switch"),
    SAFE_ADD_ADDRESS("screen_safe_add_address"),
    SAFE_ADD_NAME("screen_safe_add_name"),
    SAFE_ADD_OWNER("screen_safe_add_owner"),
    SAFE_ADD_ENS("screen_safe_add_ens"),
    SAFE_ADD_UD("screen_safe_add_ud"),
    TRANSACTIONS_NO_SAFE("screen_transactions_no_safe"),
    TRANSACTIONS_QUEUE("screen_transactions_queue"),
    TRANSACTIONS_HISTORY("screen_transactions_history"),
    TRANSACTIONS_DETAILS("screen_transactions_details"),
    TRANSACTIONS_DETAILS_ACTION("screen_transaction_details_action"),
    TRANSACTIONS_DETAILS_ACTION_LIST("screen_transaction_details_action_list"),
    TRANSACTIONS_DETAILS_ADVANCED("screen_transactions_details_advanced"),
    TRANSACTIONS_REJECTION_CONFIRMATION("screen_transactions_reject_confirmation"),
    TRANSACTIONS_EXEC_REVIEW("screen_exec_tx_review"),
    TRANSACTIONS_EXEC_REVIEW_ADVANCED("screen_exec_tx_review_advanced"),
    TRANSACTIONS_EXEC_EDIT_FEE("screen_edit_exec_tx_fee"),
    TRANSACTIONS_EXEC_SELECT_KEY("screen_select_exec_key"),
    TRANSACTIONS_SUCCESS_SIGNER("screen_tx_signer_success"),
    SETTINGS_APP("screen_settings_app"),
    SETTINGS_APP_ADVANCED("screen_settings_app_advanced"),
    SETTINGS_APP_APPEARANCE("screen_settings_app_appearance"),
    SETTINGS_APP_FIAT("screen_settings_app_edit_fiat"),
    SETTINGS_APP_CHAIN_PREFIX("screen_settings_app_chain_prefix"),
    SETTINGS_APP_PASSCODE("screen_settings_app_passcode"),
    SETTINGS_GET_IN_TOUCH("screen_settings_app_support"),
    SETTINGS_ABOUT_SAFE("screen_settings_app_about_safe"),
    SETTINGS_SAFE_NO_SAFE("screen_settings_safe_no_safe"),
    SETTINGS_SAFE("screen_settings_safe"),
    SETTINGS_SAFE_EDIT_NAME("screen_settings_safe_edit_name"),
    SETTINGS_SAFE_ADVANCED("screen_settings_safe_advanced"),
    SCANNER("screen_camera"),
    OWNER_SELECT_ACCOUNT("screen_owner_select_account"),
    OWNER_SELECT_LEDGER_ACCOUNT("screen_owner_ledger_account"),
    PASSCODE_CREATE("screen_passcode_create"),
    PASSCODE_CREATE_REPEAT("screen_passcode_create_repeat"),
    PASSCODE_CHANGE("screen_passcode_change"),
    PASSCODE_CHANGE_CREATE("screen_passcode_change_create"),
    PASSCODE_CHANGE_REPEAT("screen_passcode_change_repeat"),
    PASSCODE_ENTER("screen_passcode_enter"),
    UPDATE_DEPRECATED("screen_update_deprecated"),
    UPDATE_DEPRECATED_SOON("screen_update_deprecated_soon"),
    UPDATE_NEW_VERSION("screen_update_new_version"),
    LEDGER_DEVICE_LIST("screen_ledger_nano_x_device"),
    LEDGER_SIGN("screen_ledger_nano_x_sign")
}
