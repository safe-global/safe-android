package io.gnosis.data.backend.dto

data class DataDecodedDto(
    val method: String,
    val parameters: List<ParamsDto>?
)

data class ParamsDto(
    val name: String,
    val type: String,
    val value: String
)

enum class Operation(val id: Int) {
    CALL(0),
    DELEGATE(1)
}
