package io.gnosis.safe.utils

import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import timber.log.Timber

fun FragmentManager.getParentNavController(): NavController? {
    return try {
        (primaryNavigationFragment as NavHostFragment).navController
    } catch (e: Exception) {
        Timber.e(e)
        null
    }
}
