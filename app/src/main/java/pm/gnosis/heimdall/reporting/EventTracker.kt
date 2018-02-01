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
    CREATE_TRANSACTION,
    CHANGE_PASSWORD,
    GENERATE_MNEMONIC,
    NETWORK_SETTINGS,
    ONBOARDING_INTRO,
    PASSWORD_SETUP,
    RECEIPT_TRANSACTION,
    RESTORE_ACCOUNT,
    REVEAL_MNEMONIC,
    SAFE_DETAILS,
    SAFE_OVERVIEW,
    SAFE_SETTINGS,
    SECURITY_SETTINGS,
    SETTINGS,
    SETUP_INTRO,
    SIGN_TRANSACTION,
    SPLASH,
    SUBMIT_TRANSACTION,
    TOKEN_INFO,
    TOKEN_MANAGEMENT,
    UNLOCK,

    DIALOG_REQUEST_SIGNATURE,
    DIALOG_SHARE_SAFE,
    DIALOG_SHARE_ADDRESS
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