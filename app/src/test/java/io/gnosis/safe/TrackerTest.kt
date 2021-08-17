package io.gnosis.safe

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.gnosis.safe.Tracker.ParamValues.PUSH_DISABLED
import io.gnosis.safe.Tracker.ParamValues.PUSH_ENABLED
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

class TrackerTest {

    val firebaseAnalytics = mockk<FirebaseAnalytics>(relaxed = true)
    val firebaseCrashlytics = mockk<FirebaseCrashlytics>(relaxed = true)

    val tracker = Tracker(firebaseAnalytics, firebaseCrashlytics)

    @Before
    fun setUp() {
    }

    @Test
    fun setNumSafes() {

        tracker.setNumSafes(1)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(Tracker.Param.NUM_SAFES, 1.toString()) }
    }

    @Test
    fun setNumKeysGenerated() {

        tracker.setNumKeysGenerated(1)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(Tracker.Param.NUM_KEYS_GENERATED, 1.toString()) }
    }

    @Test
    fun setNumKeysImported() {

        tracker.setNumKeysImported(1)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(Tracker.Param.NUM_KEYS_IMPORTED, 1.toString()) }
    }

    @Test
    fun `setPushInfo(true) - enabled`() {

        tracker.setPushInfo(true)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(Tracker.Param.PUSH_INFO, PUSH_ENABLED) }
    }

    @Test
    fun `setPushInfo(false) - disabled`() {

        tracker.setPushInfo(false)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(Tracker.Param.PUSH_INFO, PUSH_DISABLED) }
    }

    @Test
    fun `setPasscodeIsSet(true) - enabled`() {

        tracker.setPasscodeIsSet(true)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(Tracker.Param.PASSCODE_IS_SET, "true") }
    }

    @Test
    fun `setPasscodeIsSet(false) - disabled`() {

        tracker.setPasscodeIsSet(false)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(Tracker.Param.PASSCODE_IS_SET, "false") }
    }

    @Test
    fun logScreen() {

        tracker.logScreen(ScreenId.ASSETS_COINS, BigInteger.ONE)

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(ScreenId.ASSETS_COINS.value, any()) }
    }

    @Test
    fun `logScreen (logEvent throws exception) expect recordException to be called`() {

        val exception = RuntimeException("fnord")
        every { firebaseAnalytics.logEvent(any(), any()) } throws exception

        tracker.logScreen(ScreenId.ASSETS_COINS, BigInteger.ONE)

        verify(exactly = 1) { firebaseAnalytics.logEvent(ScreenId.ASSETS_COINS.value, any()) }
        verify(exactly = 1) { firebaseCrashlytics.recordException(exception) }
    }

    @Test
    fun logSafeAdded() {

        tracker.logSafeAdded(BigInteger.ONE)

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.SAFE_ADDED, any()) }
    }

    @Test
    fun logSafeRemoved() {

        tracker.logSafeRemoved(BigInteger.ONE)

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.SAFE_REMOVED, any()) }
    }

    @Test
    fun logKeyGenerated() {

        tracker.logKeyGenerated()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.KEY_GENERATED, any()) }
    }

    @Test
    fun logKeyImported() {

        tracker.logKeyImported(true)

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.KEY_IMPORTED, any()) }
    }

    @Test
    fun logKeyDeleted() {

        tracker.logKeyDeleted()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.KEY_DELETED, any()) }
    }

    @Test
    fun logTransactionConfirmed() {

        tracker.logTransactionConfirmed(BigInteger.ONE)

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.TRANSACTION_CONFIRMED, any()) }
    }

    @Test
    fun logTransactionRejected() {

        tracker.logTransactionRejected(BigInteger.ONE)

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.TRANSACTION_REJECTED, any()) }
    }

    @Test
    fun logBannerPasscodeSkip() {

        tracker.logBannerPasscodeSkip()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_PASSCODE_SKIP, any()) }
    }

    @Test
    fun logBannerPasscodeCreate() {

        tracker.logBannerPasscodeCreate()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_PASSCODE_CREATE, any()) }
    }

    @Test
    fun logBannerOwnerSkip() {

        tracker.logBannerOwnerSkip()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_OWNER_SKIP, any()) }
    }

    @Test
    fun logBannerOwnerImport() {
        tracker.logBannerOwnerImport()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_OWNER_IMPORT, any()) }
    }

    @Test
    fun logOnboardingOwnerSkipped() {
        tracker.logOnboardingOwnerSkipped()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.ONBOARDING_OWNER_SKIPPED, any()) }
    }

    @Test
    fun logOnboardingOwnerImport() {
        tracker.logOnboardingOwnerImport()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.ONBOARDING_OWNER_IMPORT, any()) }
    }

    @Test
    fun logPasscodeEnabled() {
        tracker.logPasscodeEnabled()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_ENABLED, any()) }
    }

    @Test
    fun logPasscodeDisabled() {
        tracker.logPasscodeDisabled()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_DISABLED, any()) }
    }

    @Test
    fun logPasscodeReset() {
        tracker.logPasscodeReset()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_RESET, any()) }
    }

    @Test
    fun logPasscodeSkipped() {
        tracker.logPasscodeSkipped()

        // We cannot look inside Bundles in unit tests :-(
        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_SKIPPED, any()) }
    }

    @Test
    fun logException() {
        val e = RuntimeException("Fnord")
        tracker.logException(e)

        verify(exactly = 1) { firebaseCrashlytics.recordException(e) }
    }
}
