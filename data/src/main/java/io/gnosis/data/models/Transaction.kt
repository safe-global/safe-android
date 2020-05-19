package io.gnosis.data.models

sealed class Transaction(open val nonce: String) {

    data class ChangeMastercopy(override val nonce: String) : Transaction(nonce)
    data class ChangeMastercopyQueued(override val nonce: String) : Transaction(nonce)
    data class SettingsChange(override val nonce: String) : Transaction(nonce)
    data class SettingsChangeQueued(override val nonce: String) : Transaction(nonce)
    data class Transfer(override val nonce: String) : Transaction(nonce)
    data class TransferQueued(override val nonce: String) : Transaction(nonce)
}
