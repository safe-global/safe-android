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
       // if (preRelease != null) require(preRelease.matches(Regex("""[\dA-z\-]+(?:\.[\dA-z\-]+)*"""))) { "Pre-release version is not valid" }
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

        fun parse(version: String): SemVer {
            //val pattern = Regex("""(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?(?:-([\dA-z\-]+(?:\.[\dA-z\-]+)*))?""")
            val pattern = Regex("""(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?(?:\.)?(0|[1-9]\d*)?(?:-((0|[1-9]\d*(?:rc)?-)?([A-z\_]+)*))?""")
            val result = pattern.matchEntire(version) ?: throw IllegalArgumentException("Invalid version string [$version]")
            return SemVer(
                major = if (result.groupValues[1].isEmpty()) 0 else result.groupValues[1].toInt(),
                minor = if (result.groupValues[2].isEmpty()) 0 else result.groupValues[2].toInt(),
                patch = if (result.groupValues[3].isEmpty()) 0 else result.groupValues[3].toInt(),
                preRelease = if (result.groupValues[4].isEmpty()) null else result.groupValues[4]
            )
        }

        fun parseRange(range: String): Pair<SemVer, SemVer?> {
           // val pattern = Regex("""([\dA-z\.]+)(?:-)?([\dA-z\.]+)?""")
            val flavorPatter = Regex("""([\d\.]+)(-)(-[A-z]*)?""")
            val flavorResult = flavorPatter.matchEntire(range)
            val flavor = flavorResult?.let { it.groupValues[2] }
            if (flavor != null && flavor.isNotEmpty()) {
                val lastFlavorIndex = range.lastIndexOf(flavor)
                val restOfRange = range.substring(lastFlavorIndex, range.length)
                return if (restOfRange.startsWith("-")) {
                    Pair(SemVer.parse(range.substring(0, range.indexOf(flavor))), SemVer.parse(restOfRange.substring(1, restOfRange.length)))
                } else {
                    Pair(SemVer.parse(range), null)
                }
            } else {
                val pattern = Regex("""([\dA-z\.]+)(?:-)?([\dA-z\.]+)?""")
                val result = pattern.matchEntire(range) ?: throw IllegalArgumentException("Invalid range version string [$range]")
                return when {
                    result.groupValues[2].isEmpty() -> Pair(SemVer.parse(result.groupValues[1]), null)
                    else -> Pair(SemVer.parse(result.groupValues[1]), SemVer.parse(result.groupValues[2]))
                }
            }


        }
    }
}
