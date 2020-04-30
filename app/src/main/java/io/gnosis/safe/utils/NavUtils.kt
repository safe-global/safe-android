package io.gnosis.safe.utils

import androidx.fragment.app.FragmentManager
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment

fun navigateFromChild(childFragmentManager: FragmentManager, directions: NavDirections) {
    try {
        (childFragmentManager.primaryNavigationFragment as NavHostFragment).navController.navigate(directions)
    } catch (e: Exception) {
        // ignore as current content is not start of the directions
    }
}

