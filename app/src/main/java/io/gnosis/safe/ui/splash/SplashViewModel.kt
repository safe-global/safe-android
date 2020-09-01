package io.gnosis.safe.ui.splash

import android.content.Context
import android.content.Intent
import io.gnosis.data.repositories.SafeRepository
import io.gnosis.safe.Tracker
import io.gnosis.safe.di.ApplicationContext
import io.gnosis.safe.notifications.NotificationRepository
import io.gnosis.safe.ui.StartActivity
import io.gnosis.safe.ui.base.AppDispatchers
import io.gnosis.safe.ui.base.BaseStateViewModel
import io.gnosis.safe.ui.terms.TermsChecker
import javax.inject.Inject

class SplashViewModel
@Inject
constructor(
    private val notificationRepository: NotificationRepository,
    private val safeRepository: SafeRepository,
    private val tracker: Tracker,
    private val termsChecker: TermsChecker,
    appDispatchers: AppDispatchers,
    @ApplicationContext private val appContext: Context
) : BaseStateViewModel<SplashViewModel.TermsAgreed>(appDispatchers) {

    override fun initialState(): TermsAgreed = TermsAgreed(null)

    suspend fun onAppStart() {

        notificationRepository.register()

        notificationRepository.clearNotifications()
        val pushEnabled = notificationRepository.checkPermissions()
        tracker.setPushInfo(pushEnabled)

        val numSafes = safeRepository.getSafeCount()
        tracker.setNumSafes(numSafes)
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
