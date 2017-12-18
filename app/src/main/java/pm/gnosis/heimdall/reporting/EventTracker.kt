package pm.gnosis.heimdall.reporting

import io.reactivex.Single


interface EventTracker {
    fun submit(event: Event)

    fun loadTrackingIdentifier(): Single<String>
}

sealed class Event {
    class SubmittedTransaction : Event()
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
    GENERATE_MNEMONIC,
    NETWORK_SETTINGS,
    ONBOARDING_INTRO,
    PASSWORD_SETUP,
    RESTORE_ACCOUNT,
    SAFE_DETAILS,
    SAFE_OVERVIEW,
    SECURITY_SETTINGS,
    SETTINGS,
    SETUP_INTRO,
    SPLASH,
    TOKEN_INFO,
    TOKEN_MANAGEMENT,
    UNLOCK,
    VIEW_TRANSACTION,

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
    SAFE_DETAILS_SETTINGS,
}