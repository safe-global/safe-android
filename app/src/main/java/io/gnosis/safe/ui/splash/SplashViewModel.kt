package io.gnosis.safe.ui.splash

import android.content.Context
import android.content.Intent
import android.os.Build
import io.gnosis.data.models.Owner
import io.gnosis.data.repositories.CredentialsRepository
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.ApplicationContext
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.settings.app.SettingsHandler
import io.gnosis.safe.ui.terms.TermsChecker
import io.gnosis.safe.workers.WorkRepository
import javax.inject.Inject

class SplashViewModel
@Inject
constructor(
    private val notificationRepository: NotificationRepository,
    private val safeRepository: SafeRepository,
    private val credentialsRepository: CredentialsRepository,
    private val workRepository: WorkRepository,
    private val tracker: Tracker,
    private val termsChecker: TermsChecker,
    private val settingsHandler: SettingsHandler,
    appDispatchers: AppDispatchers,
    @ApplicationContext private val appContext: Context
) : BaseStateViewModel<SplashViewModel.TermsAgreed>(appDispatchers) {

    override fun initialState(): TermsAgreed = TermsAgreed(null)

    suspend fun onAppStart() {

        val currentVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).longVersionCode
        } else {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionCode.toLong()
        }
        val appUpdated = settingsHandler.currentVersion < currentVersion

        if (appUpdated) {
            settingsHandler.showWhatsNew = true
            settingsHandler.currentVersion = currentVersion
        }

        workRepository.registerForPushNotifications()
        workRepository.updateChainInfo()

        notificationRepository.clearNotifications()
        val pushEnabled = notificationRepository.checkPermissions()
        tracker.setPushInfo(pushEnabled)

        val numSafes = safeRepository.getSafeCount()
        tracker.setNumSafes(numSafes)
        tracker.setNumKeysImported(credentialsRepository.ownerCount(Owner.Type.IMPORTED))
        tracker.setNumKeysGenerated(credentialsRepository.ownerCount(Owner.Type.GENERATED))
    }

    fun onStartClicked() {
        safeLaunch {
            if (termsChecker.getTermsAgreed()) {
                updateState { TermsAgreed(viewAction = ViewAction.StartActivity(Intent(appContext, StartActivity::class.java))) }
            } else {
                updateState(true) { TermsAgreed(ShowTerms) }
            }
        }
    }

    fun handleAgreeClicked() {
        safeLaunch {
            termsChecker.setTermsAgreed(true)
            updateState { TermsAgreed(viewAction = ViewAction.StartActivity(Intent(appContext, StartActivity::class.java))) }
        }
    }

    fun skipGetStartedButtonWhenTermsAgreed() {
        safeLaunch {
            if (termsChecker.getTermsAgreed()) {
                updateState { TermsAgreed(viewAction = ViewAction.StartActivity(Intent(appContext, StartActivity::class.java))) }
            } else {
                updateState { TermsAgreed(ShowButton) }
            }
        }
    }

    data class TermsAgreed(override var viewAction: ViewAction?) : State

    object ShowTerms : ViewAction
    object ShowButton : ViewAction
}
