package pm.gnosis.heimdall.reporting

import io.reactivex.Single


interface EventTracker {
    fun submit(event: Event)

    fun loadTrackingIdentifier(): Single<String>
}

sealed class Event {
    class SubmittedTransaction : Event()
    class SignedTransaction : Event()
    data class ScreenView(val id: ScreenId) : Event()
    data class ButtonClick(val id: ButtonId) : Event()
    data class TabSelect(val id: TabId) : Event()
}

enum class ScreenId {
    ACCOUNT,
    ACCOUNT_SETUP,
    ADD_SAFE,
    ADD_TOKEN,
    ADDRESS_BOOK,
    ADDRESS_BOOK_ENTRY,
    ADDRESS_BOOK_ENTRY_DETAILS,
    AUTHENTICATE,
    BUY_CREDITS,
    CHANGE_PASSWORD,
    CONFIRM_SAFE_RECOVERY_PHRASE,
    CONFIRM_TRANSACTION,
    CREATE_ASSET_TRANSFER,
    CREATE_SAFE,
    CREATE_SAFE_INTRO,
    CREATE_TRANSACTION,
    DEBUG_SETTINGS,
    FINGERPRINT_SETUP,
    GENERATE_MNEMONIC,
    NETWORK_SETTINGS,
    ONBOARDING_INTRO,
    PAIRING,
    PASSWORD_CONFIRM,
    PASSWORD_SETUP,
    QR_SCAN,
    RECEIPT_TRANSACTION,
    RECEIVE_TOKEN,
    RECOVERY_ACTIVITY,
    RESTORE_ACCOUNT,
    REVEAL_MNEMONIC,
    REVIEW_TRANSACTION,
    SAFE_MAIN,
    SAFE_DETAILS,
    SAFE_OVERVIEW,
    SAFE_RECOVERY_PHRASE,
    SAFE_SETTINGS,
    SECURITY_SETTINGS,
    SELECT_SAFE,
    SELECT_TOKEN,
    SETTINGS,
    SETUP_INTRO,
    SIGN_TRANSACTION,
    SPLASH,
    SUBMIT_TRANSACTION,
    TOKEN_INFO,
    TOKEN_MANAGEMENT,
    TRANSACTION_STATUS,
    UNLOCK,

    DIALOG_REQUEST_SIGNATURE,
    DIALOG_SHARE_SAFE,
    DIALOG_SHARE_ADDRESS,
}

enum class ButtonId {
    SAFE_DETAILS_CREATE_TRANSACTION,
    SAFE_OVERVIEW_SCAN_TRANSACTION,
}

enum class TabId {
    ADD_NEW_SAFE,
    ADD_SAFE_EXISTING,
    SAFE_DETAILS_ASSETS,
    SAFE_DETAILS_TRANSACTIONS,
}
