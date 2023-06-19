package io.gnosis.safe

import android.app.Application
import io.intercom.android.sdk.Intercom
import io.intercom.android.sdk.UnreadConversationCountListener
import timber.log.Timber

object HeimdallIntercom {

    fun setup(application: Application) {
        try {
            Intercom.initialize(
                application,
                BuildConfig.INTERCOM_API_KEY,
                BuildConfig.INTERCOM_APP_ID
            )
            Intercom.client().loginUnidentifiedUser()
            Intercom.client().setInAppMessageVisibility(Intercom.Visibility.GONE)
        } catch (e: Exception) {
            Timber.e(e, "Intercom setup failed.")
        }
    }

    fun unreadConversationCount(): Int {
        return kotlin.runCatching {
            Intercom.client().unreadConversationCount
        }.getOrElse {
            // fail silently
            0
        }
    }

    fun addUnreadConversationCountListener(listener: UnreadConversationCountListener) {
        try {
            Intercom.client().addUnreadConversationCountListener(listener)
        } catch (e: Exception) {
            // fail silently
        }
    }

    fun removeUnreadConversationCountListener(listener: UnreadConversationCountListener) {
        try {
            Intercom.client().removeUnreadConversationCountListener(listener)
        } catch (e: Exception) {
            // fail silently
        }
    }

    fun handlePushMessage() {
        try {
            Intercom.client().handlePushMessage()
        } catch (e: Exception) {
            // fail silently
        }
    }

    fun present() {
        Intercom.client().present()
    }
}
