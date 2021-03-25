package io.gnosis.safe

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Singleton

@Singleton
class Tracker private constructor(context: Context) {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    fun setNumSafes(numSafes: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_SAFES, numSafes.toString())
    }

    fun setNumKeysImported(numKeysImported: Int) {
        firebaseAnalytics.setUserProperty(Param.NUM_KEYS_IMPORTED, numKeysImported.toString())
    }

    fun setPushInfo(enabled: Boolean) {
        firebaseAnalytics.setUserProperty(Param.PUSH_INFO, if (enabled) ParamValues.PUSH_ENABLED else ParamValues.PUSH_DISABLED)
    }

    fun setPasscodeIsSet(usePasscode: Boolean) {
        firebaseAnalytics.setUserProperty(Param.PASSCODE_IS_SET, usePasscode.toString())
    }

    fun logScreen(screenId: ScreenId) {
        logEvent(screenId.value, null)
    }

    fun logKeyImported(usingSeedPhrase: Boolean) {
        logEvent(
            Event.KEY_IMPORTED, mapOf(
                Param.KEY_IMPORT_TYPE to if (usingSeedPhrase) ParamValues.KEY_IMPORT_TYPE_SEED else ParamValues.KEY_IMPORT_TYPE_KEY
            )
        )
    }

    fun logTransactionConfirmed() {
        logEvent(Event.TRANSACTION_CONFIRMED, null)
    }

    fun logTransactionRejected() {
        logEvent(Event.TRANSACTION_REJECTED, null)
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

    private fun logEvent(name: String, attrs: Map<String, Any?>?) {
        try {
            val bundle = Bundle()
            attrs?.let {
                for ((key, value) in attrs) {
                    when (value) {
                        is String -> bundle.putString(key, value)
                        is Long -> bundle.putLong(key, value)
                        is Int -> bundle.putInt(key, value)
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
        FirebaseCrashlytics.getInstance().recordException(exception)
    }

    object Event {
        val KEY_IMPORTED = "user_key_imported"
        val TRANSACTION_CONFIRMED = "user_transaction_confirmed"
        val TRANSACTION_REJECTED = "user_transaction_rejected"
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

    }

    object Param {
        val NUM_SAFES = "num_safes"
        val PUSH_INFO = "push_info"
        val NUM_KEYS_IMPORTED = "num_keys_imported"
        val KEY_IMPORT_TYPE = "import_type"
        val PASSCODE_IS_SET = "passcode_is_set"
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

        fun getInstance(context: Context): Tracker =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Tracker(context).also { INSTANCE = it }
            }
    }
}

enum class ScreenId(val value: String) {
    LAUNCH("screen_launch"),
    LAUNCH_TERMS("screen_launch_terms"),
    ASSETS_NO_SAFE("screen_assets_no_safe"),
    ASSETS_COINS("screen_assets_coins"),
    ASSETS_COLLECTIBLES("screen_assets_collectibles"),
    ASSETS_COLLECTIBLES_DETAILS("screen_assets_collectibles_details"),
    OWNER_INFO("screen_owner_info"),
    OWNER_ENTER_SEED("screen_owner_enter_seed"),
    SAFE_RECEIVE("screen_safe_receive"),
    SAFE_SELECT("screen_safe_switch"),
    SAFE_ADD_ADDRESS("screen_safe_add_address"),
    SAFE_ADD_NAME("screen_safe_add_name"),
    SAFE_ADD_OWNER("screen_safe_add_owner"),
    SAFE_ADD_ENS("screen_safe_add_ens"),
    TRANSACTIONS_NO_SAFE("screen_transactions_no_safe"),
    TRANSACTIONS_QUEUE("screen_transactions_queue"),
    TRANSACTIONS_HISTORY("screen_transactions_history"),
    TRANSACTIONS_DETAILS("screen_transactions_details"),
    TRANSACTIONS_DETAILS_ACTION("screen_transaction_details_action"),
    TRANSACTIONS_DETAILS_ACTION_LIST("screen_transaction_details_action_list"),
    TRANSACTIONS_DETAILS_ADVANCED("screen_transactions_details_advanced"),
    TRANSACTIONS_REJECTION_CONFIRMATION("screen_transactions_reject_confirmation"),
    SETTINGS_APP("screen_settings_app"),
    SETTINGS_APP_ADVANCED("screen_settings_app_advanced"),
    SETTINGS_APP_APPEARANCE("screen_settings_app_appearance"),
    SETTINGS_APP_FIAT("screen_settings_app_edit_fiat"),
    SETTINGS_APP_PASSCODE("screen_settings_app_passcode"),
    SETTINGS_GET_IN_TOUCH("screen_settings_app_support"),
    SETTINGS_SAFE_NO_SAFE("screen_settings_safe_no_safe"),
    SETTINGS_SAFE("screen_settings_safe"),
    SETTINGS_SAFE_EDIT_NAME("screen_settings_safe_edit_name"),
    SETTINGS_SAFE_ADVANCED("screen_settings_safe_advanced"),
    SCANNER("screen_camera"),
    OWNER_SELECT_ACCOUNT("screen_owner_select_account"),
    CREATE_PASSCODE("screen_passcode_create"),
    REPEAT_PASSCODE("screen_passcode_create_repeat")
}
