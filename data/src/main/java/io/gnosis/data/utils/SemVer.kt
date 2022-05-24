package io.gnosis.data.utils

data class SemVer(
    val major: Int = 0,
    val minor: Int = 0,
    val patch: Int = 0,
    val preRelease: String? = null,
    val buildMetadata: String? = null
) : Comparable<SemVer> {

    init {
        require(major >= 0) { "Major version must be a positive number" }
        require(minor >= 0) { "Minor version must be a positive number" }
        require(patch >= 0) { "Patch version must be a positive number" }
        if (preRelease != null) require(preRelease.matches(Regex(REGEX_PRE_RELEASE))) { "Pre-release version is not valid" }
        if (buildMetadata != null) require(buildMetadata.matches(Regex(REGEX_BUILD_META))) { "Build metadata is not valid" }
    }

    fun isInside(rangesList: String, ignoreExtensions: Boolean = false): Boolean {
        val version = if (ignoreExtensions) this.copy(preRelease = null) else this
        var checkResult = false
        run loop@{
            rangesList.split(",").filter { it.isNotEmpty() }.forEach {
                val range = parseRange(it, ignoreExtensions)
                when {
                    range.second != null && version > range.second!! -> {
                        return@forEach
                    }
                    range.second != null && range.second!! >= version && range.first <= version -> {
                        checkResult = true
                        return@loop
                    }
                    range.second == null && range.first == version -> {
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

        private const val REGEX_BUILD_META = """([0-9A-Za-z-])*"""
        private const val REGEX_PRE_RELEASE = """(0|[1-9]\d*(?:rc)?-)?([A-z\_]+)*"""
        private const val REGEX_SEM_VER = "(0|[1-9]\\d*)?(?:\\.)?(0|[1-9]\\d*)?(?:\\.)?(0|[1-9]\\d*)?(-$REGEX_PRE_RELEASE)?(\\+$REGEX_BUILD_META)?"
        private const val REGEX_SEM_VER_RANGE = "(${REGEX_SEM_VER})(?:\\.\\.\\.)?(?:(${REGEX_SEM_VER}))?"

        fun parse(version: String, ignoreExtensions: Boolean = false): SemVer {
            val pattern = Regex(REGEX_SEM_VER)
            val result = pattern.matchEntire(version.trim()) ?: throw IllegalArgumentException("Invalid version string [$version]")
            return SemVer(
                major = if (result.groupValues[1].isEmpty()) 0 else result.groupValues[1].toInt(),
                minor = if (result.groupValues[2].isEmpty()) 0 else result.groupValues[2].toInt(),
                patch = if (result.groupValues[3].isEmpty()) 0 else result.groupValues[3].toInt(),
                preRelease = if (ignoreExtensions || result.groupValues[4].isEmpty()) null else "${result.groupValues[4].substring(1)}",
                buildMetadata = if (ignoreExtensions || result.groupValues[7].isEmpty()) null else "${result.groupValues[7].substring(1)}"
            )
        }

        fun parseRange(range: String, ignoreExtensions: Boolean = false): Pair<SemVer, SemVer?> {
            val pattern = Regex(REGEX_SEM_VER_RANGE)
            val result = pattern.matchEntire(range.trim()) ?: throw IllegalArgumentException("Invalid range version string [$range]")
            return when {
                result.groupValues[10].isEmpty() -> Pair(parse(result.groupValues[1], ignoreExtensions), null)
                else -> Pair(parse(result.groupValues[1], ignoreExtensions), parse(result.groupValues[10], ignoreExtensions))
            }
        }
    }
}
