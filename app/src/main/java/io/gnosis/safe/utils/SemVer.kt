package io.gnosis.safe.utils

data class SemVer(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val preRelease: String? = null
) : Comparable<SemVer> {

    init {
        require(major >= 0) { "Major version must be a positive number" }
        require(minor >= 0) { "Minor version must be a positive number" }
        require(patch >= 0) { "Patch version must be a positive number" }
        if (preRelease != null) require(preRelease.matches(Regex("""(0|[1-9]\d*(?:rc)?-)?([A-z\_]+)*"""))) { "Pre-release version is not valid" }
    }

    fun isInside(rangesList: String): Boolean {
        var checkResult = false
        run loop@{
            rangesList.split(",").filter { it.isNotEmpty() }.forEach {
                val range = parseRange(it)
                when {
                    range.second != null && this > range.second!! -> {
                        return@forEach
                    }
                    range.second != null && range.second!! >= this && range.first <= this -> {
                        checkResult = true
                        return@loop
                    }
                    range.second == null && range.first == this -> {
                        checkResult = true
                        return@loop
                    }
                    else -> {
                        return@forEach
                    }
                }
            }
        }
        return checkResult
    }

    override fun compareTo(other: SemVer): Int = when {
        major > other.major -> 1
        major < other.major -> -1
        minor > other.minor -> 1
        minor < other.minor -> -1
        patch > other.patch -> 1
        patch < other.patch -> -1
        preRelease == null && other.preRelease == null -> 0
        preRelease != null && other.preRelease == null -> -1
        preRelease == null && other.preRelease != null -> 1
        else -> {
            val parts = preRelease.orEmpty()
            val otherParts = other.preRelease.orEmpty()
            parts.compareTo(otherParts)
        }
    }

    private fun String.isNumeric(): Boolean = this.matches(Regex("""\d+"""))

    companion object {

        private const val REGEX_PRE_RELEASE = """(0|[1-9]\d*(?:rc)?-)?([A-z\_]+)*"""
        private const val REGEX_SEM_VER = "(0|[1-9]\\d*)?(?:\\.)?(0|[1-9]\\d*)?(?:\\.)?(0|[1-9]\\d*)?(?:-$REGEX_PRE_RELEASE)?"
        private const val REGEX_SEM_VER_RANGE = "(${REGEX_SEM_VER})(?:\\.\\.\\.)?(?:(${REGEX_SEM_VER}))?"

        fun parse(version: String): SemVer {
            val pattern = Regex(REGEX_SEM_VER)
            val result = pattern.matchEntire(version) ?: throw IllegalArgumentException("Invalid version string [$version]")
            return SemVer(
                major = if (result.groupValues[1].isEmpty()) 0 else result.groupValues[1].toInt(),
                minor = if (result.groupValues[2].isEmpty()) 0 else result.groupValues[2].toInt(),
                patch = if (result.groupValues[3].isEmpty()) 0 else result.groupValues[3].toInt(),
                preRelease = if (result.groupValues[5].isEmpty()) null else "${result.groupValues[4]}${result.groupValues[5]}"
            )
        }

        fun parseRange(range: String): Pair<SemVer, SemVer?> {
            val pattern = Regex(REGEX_SEM_VER_RANGE)
            val result = pattern.matchEntire(range) ?: throw IllegalArgumentException("Invalid range version string [$range]")
            return when {
                result.groupValues[7].isEmpty() -> Pair(SemVer.parse(result.groupValues[1]), null)
                else -> Pair(parse(result.groupValues[1]), SemVer.parse(result.groupValues[7]))
            }
        }
    }
}
