package pm.gnosis.heimdall.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.accounts.base.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.SignaturePushRepository
import pm.gnosis.heimdall.ui.transactions.SignTransactionActivity
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.removeHexPrefix
import timber.log.Timber
import javax.inject.Inject

class HeimdallFirebaseService : FirebaseMessagingService() {

    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager }

    @Inject
    lateinit var accountsRepo: AccountsRepository

    @Inject
    lateinit var signaturePushRepository: SignaturePushRepository

    private val disposables = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()
        HeimdallApplication[this].component.inject(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // No data received
        if (message.data.isEmpty()) return

        val uri = message.data["uri"] ?: return
        val parsed = GnoSafeUrlParser.parse(uri)
        when (parsed) {
            is GnoSafeUrlParser.Parsed.SignRequest -> handleSignRequest(message, parsed)
            is GnoSafeUrlParser.Parsed.SignResponse -> handleSignResponse(message, parsed)
        }
    }

    private fun handleSignResponse(message: RemoteMessage, parsed: GnoSafeUrlParser.Parsed.SignResponse) {
        signaturePushRepository.receivedSignature(message.from, parsed.signature)
    }

    private fun handleSignRequest(message: RemoteMessage, parsed: GnoSafeUrlParser.Parsed.SignRequest) {
        val targets = message.data["targets"]?.split(",") ?: return
        if (targets.isEmpty()) return

        disposables += accountsRepo.loadActiveAccount()
                .subscribe({
                    if (targets.contains(it.address.asEthereumAddressString().removeHexPrefix().toLowerCase())) {
                        showNotification(parsed)
                    }
                }, Timber::e)
    }

    private fun showNotification(signRequest: GnoSafeUrlParser.Parsed.SignRequest) {
        val intent = SignTransactionActivity.createIntent(this, signRequest.safe, signRequest.transaction, true)
        val builder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_gno)
                .setContentTitle(getString(R.string.sign_transaction_request_title))
                .setContentText(getString(R.string.sign_transaction_request_message))
                .setAutoCancel(true)
                .setVibrate(VIBRATE_PATTERN)
                .setLights(LIGHT_COLOR, 100, 100)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.priority = NotificationManagerCompat.IMPORTANCE_HIGH
        }

        notificationManager?.notify(signRequest.transactionHash.hashCode(), builder.build())
    }

    companion object {
        private val VIBRATE_PATTERN = longArrayOf(0, 100, 50, 100)
        private const val LIGHT_COLOR = Color.MAGENTA
    }
}