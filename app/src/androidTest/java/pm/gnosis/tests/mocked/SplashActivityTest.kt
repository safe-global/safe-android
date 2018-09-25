package pm.gnosis.tests.mocked

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.mock
import pm.gnosis.heimdall.data.repositories.AddressBookRepository
import pm.gnosis.heimdall.data.repositories.GnosisSafeRepository
import pm.gnosis.heimdall.helpers.AddressHelper
import pm.gnosis.heimdall.ui.onboarding.OnboardingIntroActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainContract
import pm.gnosis.heimdall.ui.splash.SplashActivity
import pm.gnosis.heimdall.ui.splash.SplashContract
import pm.gnosis.heimdall.ui.splash.StartMain
import pm.gnosis.heimdall.ui.splash.StartPasswordSetup
import pm.gnosis.tests.BaseUiTest

@RunWith(AndroidJUnit4::class)
class SplashActivityTest: BaseUiTest() {
    @JvmField
    @Rule
    val activityRule = ActivityTestRule(SplashActivity::class.java, true, false)

    private val addressBookRepoMock = mock(AddressBookRepository::class.java)

    private val safeRepoMock = mock(GnosisSafeRepository::class.java)

    // ViewModel mocks

    private val safeMainContract = mock(SafeMainContract::class.java)

    private val splashContract = mock(SplashContract::class.java)

    @Before
    fun setup() {
        Intents.init()
        val comp = setupBaseInjects()
        given(comp.addressHelper()).willReturn(AddressHelper(addressBookRepoMock, safeRepoMock))
        given(comp.viewModelFactory()).willReturn(object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return when (modelClass) {
                    SplashContract::class.java -> splashContract
                    SafeMainContract::class.java -> safeMainContract
                    else -> null
                } as T
            }
        })
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun startMain() {
        // SafeMain Setup
        given(encryptionManagerMock.unlocked()).willReturn(Single.just(true))
        given(safeMainContract.loadSelectedSafe()).willReturn(Single.error(NotImplementedError()))
        given(safeMainContract.observeSafes()).willReturn(Flowable.error(NotImplementedError()))

        given(splashContract.initialSetup()).willReturn(Single.just(StartMain))
        activityRule.launchActivity(null)
        // Wait for activity to start
        Thread.sleep(500)
        intended(hasComponent(SafeMainActivity::class.java.name))
    }

    @Test
    fun startPasswordSetup() {
        given(splashContract.initialSetup()).willReturn(Single.just(StartPasswordSetup))
        activityRule.launchActivity(null)
        // Wait for activity to start
        Thread.sleep(500)
        intended(hasComponent(OnboardingIntroActivity::class.java.name))
    }

    @Test
    fun error() {
        // SafeMain Setup
        given(encryptionManagerMock.unlocked()).willReturn(Single.just(true))
        given(safeMainContract.loadSelectedSafe()).willReturn(Single.error(NotImplementedError()))
        given(safeMainContract.observeSafes()).willReturn(Flowable.error(NotImplementedError()))

        given(splashContract.initialSetup()).willReturn(Single.error(IllegalArgumentException()))
        activityRule.launchActivity(null)
        // Wait for activity to start
        Thread.sleep(500)
        intended(hasComponent(SafeMainActivity::class.java.name))
    }
}
