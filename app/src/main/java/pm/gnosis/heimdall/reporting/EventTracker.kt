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
    ADD_TOKEN,
    ADDRESS_BOOK,
    ADDRESS_BOOK_ENTRY,
    ADDRESS_BOOK_ENTRY_DETAILS,
    CHANGE_PASSWORD,
    CONFIRM_SAFE_RECOVERY_PHRASE,
    CONFIRM_TRANSACTION,
    CREATE_ASSET_TRANSFER,
    CREATE_SAFE_INTRO,
    DEBUG_SETTINGS,
    FINGERPRINT_SETUP,
    NETWORK_SETTINGS,
    ONBOARDING_INTRO,
    PAIRING,
    PASSWORD_CONFIRM,
    PASSWORD_SETUP,
    QR_SCAN,
    RECEIVE_TOKEN,
    REVIEW_TRANSACTION,
    SAFE_MAIN,
    SAFE_RECOVERY_PHRASE,
    SECURITY_SETTINGS,
    SELECT_SAFE,
    SELECT_TOKEN,
    SPLASH,
    TOKEN_INFO,
    TOKEN_MANAGEMENT,
    TRANSACTION_STATUS,
    UNLOCK,

    DIALOG_SHARE_SAFE,
    DIALOG_SHARE_ADDRESS,
}

enum class ButtonId

enum class TabId {
    SAFE_DETAILS_ASSETS,
    SAFE_DETAILS_TRANSACTIONS,
}
