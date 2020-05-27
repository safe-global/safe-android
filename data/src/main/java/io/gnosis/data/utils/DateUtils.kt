package io.gnosis.data.utils

fun String.formatBackendDate() = this.replace("Z", "").replace("T", ", ").split(".").first()
