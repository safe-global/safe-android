package io.gnosis.safe

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.gnosis.safe.Tracker.Param.CHAIN_ID
import io.gnosis.safe.Tracker.Param.KEY_IMPORT_TYPE
import io.gnosis.safe.Tracker.Param.PASSCODE_IS_SET
import io.gnosis.safe.Tracker.ParamValues.KEY_IMPORT_TYPE_KEY
import io.gnosis.safe.Tracker.ParamValues.KEY_IMPORT_TYPE_SEED
import io.gnosis.safe.Tracker.ParamValues.PUSH_DISABLED
import io.gnosis.safe.Tracker.ParamValues.PUSH_ENABLED
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
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

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(PASSCODE_IS_SET, "true") }
    }

    @Test
    fun `setPasscodeIsSet(false) - disabled`() {

        tracker.setPasscodeIsSet(false)

        verify(exactly = 1) { firebaseAnalytics.setUserProperty(PASSCODE_IS_SET, "false") }
    }

    @Test
    fun logScreen() {

        tracker.logScreen(ScreenId.ASSETS_COINS, BigInteger.ONE)

        verify(exactly = 1) { firebaseAnalytics.logEvent(ScreenId.ASSETS_COINS.value, match { it.getString(CHAIN_ID) == "1" }) }
    }

    @Test
    fun `logScreen (logEvent throws exception) expect recordException to be called`() {

        val exception = RuntimeException("fnord")
        every { firebaseAnalytics.logEvent(any(), any()) } throws exception

        tracker.logScreen(ScreenId.ASSETS_COINS, BigInteger.ONE)

        verify(exactly = 1) { firebaseAnalytics.logEvent(ScreenId.ASSETS_COINS.value, match { it.getString(CHAIN_ID) == "1" }) }
        verify(exactly = 1) { firebaseCrashlytics.recordException(exception) }
    }

    @Test
    fun logSafeAdded() {

        tracker.logSafeAdded(BigInteger.ONE)

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.SAFE_ADDED, match { it.getString(CHAIN_ID) == "1" }) }
    }

    @Test
    fun logSafeRemoved() {

        tracker.logSafeRemoved(BigInteger.ONE)

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.SAFE_REMOVED, match { it.getString(CHAIN_ID) == "1" }) }
    }

    @Test
    fun logKeyGenerated() {

        tracker.logKeyGenerated()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.KEY_GENERATED, match { it.isEmpty }) }
    }

    @Test
    fun `logKeyImported(seed)`() {

        tracker.logKeyImported(true)

        val bundle = Bundle()
        bundle.getString(KEY_IMPORT_TYPE, KEY_IMPORT_TYPE_SEED)

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.KEY_IMPORTED, match { it.getString(KEY_IMPORT_TYPE) == KEY_IMPORT_TYPE_SEED }) }
    }

    @Test
    fun `logKeyImported(key)`() {

        tracker.logKeyImported(false)

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.KEY_IMPORTED, match { it.getString(KEY_IMPORT_TYPE) == KEY_IMPORT_TYPE_KEY }) }
    }

    @Test
    fun logKeyDeleted() {

        tracker.logKeyDeleted()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.KEY_DELETED, match { it.isEmpty }) }
    }

    @Test
    fun logTransactionConfirmed() {

        tracker.logTransactionConfirmed(BigInteger.ONE)

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.TRANSACTION_CONFIRMED, match { it.getString(CHAIN_ID) == "1" }) }
    }

    @Test
    fun logTransactionRejected() {

        tracker.logTransactionRejected(BigInteger.ONE)

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.TRANSACTION_REJECTED, match { it.getString(CHAIN_ID) == "1" }) }
    }

    @Test
    fun logBannerPasscodeSkip() {

        tracker.logBannerPasscodeSkip()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_PASSCODE_SKIP, match { it.isEmpty }) }
    }

    @Test
    fun logBannerPasscodeCreate() {

        tracker.logBannerPasscodeCreate()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_PASSCODE_CREATE, match { it.isEmpty }) }
    }

    @Test
    fun logBannerOwnerSkip() {

        tracker.logBannerOwnerSkip()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_OWNER_SKIP, match { it.isEmpty }) }
    }

    @Test
    fun logBannerOwnerImport() {
        tracker.logBannerOwnerImport()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.BANNER_OWNER_IMPORT, match { it.isEmpty }) }
    }

    @Test
    fun logOnboardingOwnerSkipped() {
        tracker.logOnboardingOwnerSkipped()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.ONBOARDING_OWNER_SKIPPED, match { it.isEmpty }) }
    }

    @Test
    fun logOnboardingOwnerImport() {
        tracker.logOnboardingOwnerImport()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.ONBOARDING_OWNER_IMPORT, match { it.isEmpty }) }
    }

    @Test
    fun logPasscodeEnabled() {
        tracker.logPasscodeEnabled()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_ENABLED, match { it.isEmpty }) }
    }

    @Test
    fun logPasscodeDisabled() {
        tracker.logPasscodeDisabled()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_DISABLED, match { it.isEmpty }) }
    }

    @Test
    fun logPasscodeReset() {
        tracker.logPasscodeReset()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_RESET, match { it.isEmpty }) }
    }

    @Test
    fun logPasscodeSkipped() {
        tracker.logPasscodeSkipped()

        verify(exactly = 1) { firebaseAnalytics.logEvent(Tracker.Event.PASSCODE_SKIPPED, match { it.isEmpty }) }
    }

    @Test
    fun logException() {
        val e = RuntimeException("Fnord")
        tracker.logException(e)

        verify(exactly = 1) { firebaseCrashlytics.recordException(e) }
    }
}
