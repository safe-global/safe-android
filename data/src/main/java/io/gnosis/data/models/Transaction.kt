package io.gnosis.data.models

sealed class Transaction(open val nonce: String) {

    data class Pending(override val nonce: String) : Transaction(nonce)
    data class Cancelled(override val nonce: String) : Transaction(nonce)
    data class Success(override val nonce: String) : Transaction(nonce)
    data class Failed(override val nonce: String) : Transaction(nonce)
}
