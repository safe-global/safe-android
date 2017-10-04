package pm.gnosis.utils

//Map functions that throw exceptions into optional types
fun <T> nullOnThrow(func: () -> T): T? = try {
    func.invoke()
} catch (e: Exception) {
    null
}

fun sameSign(a: Int, b: Int) = (a * b) > 0