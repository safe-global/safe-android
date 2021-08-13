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
    fun `logScreen (logEvent throws exception) expect logException to be called`() {

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
}
