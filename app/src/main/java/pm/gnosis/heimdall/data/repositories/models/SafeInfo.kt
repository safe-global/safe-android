package pm.gnosis.heimdall.data.repositories.models

import pm.gnosis.model.Solidity
import pm.gnosis.models.Wei

data class SafeInfo(
    val address: Solidity.Address,
    val balance: Wei,
    val requiredConfirmations: Long,
    val owners: List<Solidity.Address>,
    val isOwner: Boolean,
    val modules: List<Solidity.Address>,
    val version: SemVer
)

data class SemVer (
    val major: Long,
    val minor: Long,
    val patch: Long
): Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        when {
            this.major > other.major -> 1
            this.major < other.major -> -1
            this.minor > other.minor -> 1
            this.minor < other.minor -> -1
            this.patch > other.patch -> 1
            this.patch < other.patch -> -1
            else -> 0
        }
}

fun String.toSemVer() =
    split(".").let {
        SemVer(it[0].toLong(), it.getOrNull(1)?.toLong() ?: 0, it.getOrNull(2)?.toLong() ?: 0)
    }
