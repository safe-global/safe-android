package pm.gnosis.heimdall.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import pm.gnosis.heimdall.HeimdallApplication
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.repositories.SignaturePushRepository
import pm.gnosis.heimdall.data.repositories.TransactionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.heimdall.ui.transactions.SignTransactionActivity
import pm.gnosis.heimdall.utils.GnoSafeUrlParser
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.removeHexPrefix
import timber.log.Timber
import javax.inject.Inject

class HeimdallFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var accountsRepo: AccountsRepository

    @Inject
    lateinit var signaturePushRepository: SignaturePushRepository

    @Inject
    lateinit var notificationManager: LocalNotificationManager

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
        val safeTransaction = SafeTransaction(signRequest.transaction, TransactionRepository.Operation.values()[signRequest.operation])
        val intent = SignTransactionActivity.createIntent(this, signRequest.safe, safeTransaction, true)
        notificationManager.show(
            signRequest.transactionHash.hashCode(),
            getString(R.string.sign_transaction_request_title),
            getString(R.string.sign_transaction_request_message),
            intent
        )
    }
}
