package pm.gnosis.heimdall.data.repositories.impls

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.squareup.moshi.Moshi
import io.reactivex.*
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.BuildConfig
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.db.ApplicationDb
import pm.gnosis.heimdall.data.preferences.PreferencesSafe
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.push.*
import pm.gnosis.heimdall.data.repositories.AccountsRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository.TransactionResponse
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.toInt
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.heimdall.ui.messagesigning.SignatureRequestActivity
import pm.gnosis.heimdall.ui.safe.main.SafeMainActivity
import pm.gnosis.heimdall.ui.transactions.view.confirm.ConfirmTransactionActivity
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.*
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject

class DefaultPushServiceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    gnosisAuthenticatorDb: ApplicationDb,
    private val accountsRepository: AccountsRepository,
    private val cryptoHelper: CryptoHelper,
    private val firebaseInstanceId: FirebaseInstanceId,
    private val localNotificationManager: LocalNotificationManager,
    private val moshi: Moshi,
    private val prefs: PreferencesSafe,
    private val pushServiceApi: PushServiceApi
) : PushServiceRepository {

    private val safeDao = gnosisAuthenticatorDb.gnosisSafeDao()

    private val observedTransaction = HashMap<BigInteger, ReceiveSignatureObservable>()

    private val signTypedDataSubject = PublishSubject.create<PushMessage>()

    /*
    * Situations where a sync might be needed:
    * • Account changes
    * • Token was not synced to Gnosis Safe Push Notification Service (eg.: no Internet connection, service down)
    *
    * Warning: This call might fail if the device is not unlocked (no access to ethereum account)
    */
    @SuppressLint("CheckResult") // We start this task in the context of the application. We don't keep track of the result.
    override fun syncAuthentication(forced: Boolean) {
        FirebaseInstanceIdSingle().create()
            .map { instanceId ->
                val lastSyncedData = prefs.lastSyncedData
                val currentData = bundlePushInfo(instanceId.token)
                (lastSyncedData != currentData) to instanceId.token
            }
            .flatMapCompletable { (needsSync, pushToken) ->
                if (needsSync || forced) sendAuthenticationRequest(pushToken)
                else Completable.complete()
            }
            .subscribeBy(onComplete = { Timber.d("GnosisSafePushServiceRepository: successful sync") }, onError = Timber::e)
    }

    private inner class FirebaseInstanceIdSingle : SingleOnSubscribe<InstanceIdResult> {
        override fun subscribe(emitter: SingleEmitter<InstanceIdResult>) {
            firebaseInstanceId.instanceId
                .addOnSuccessListener { emitter.onSuccess(it) }
                .addOnFailureListener { emitter.onError(it) }
        }

        fun create(): Single<InstanceIdResult> = Single.create(this)
    }

    private fun sendAuthenticationRequest(pushToken: String) =
        Single.fromCallable { Sha3Utils.keccak(bundlePushInfo(pushToken).toByteArray()) }
            .subscribeOn(Schedulers.computation())
            .flatMap { hash ->
                accountsRepository.owners()
                    .flatMap { accounts ->
                        Single.zip(accounts.map { accountsRepository.sign(it, hash) }) { sigs ->
                            sigs.mapNotNull { sig -> (sig as? Signature)?.let { s -> ServiceSignature.fromSignature(s) } }
                        }
                    }
            }
            .map {
                PushServiceAuth(
                    pushToken = pushToken,
                    buildNumber = BuildConfig.VERSION_CODE,
                    versionName = BuildConfig.VERSION_NAME,
                    client = CLIENT,
                    bundle = BuildConfig.APPLICATION_ID,
                    signatures = it
                )
            }
            .flatMapCompletable { pushServiceApi.auth(it) }
            .doOnComplete {
                prefs.lastSyncedData = bundlePushInfo(pushToken)
            }

    // sha3("GNO" + <pushToken> + <build_number> + <version_name> + <client> + <bundle>)
    private fun bundlePushInfo(pushToken: String) =
        "$SIGNATURE_PREFIX$pushToken${BuildConfig.VERSION_CODE}${BuildConfig.VERSION_NAME}$CLIENT${BuildConfig.APPLICATION_ID}"

    override fun pair(
        temporaryAuthorization: PushServiceTemporaryAuthorization,
        signingSafe: Solidity.Address?
    ): Single<Pair<AccountsRepository.SafeOwner, Solidity.Address>> =
        Single.fromCallable {
            cryptoHelper.recover(
                Sha3Utils.keccak("$SIGNATURE_PREFIX${temporaryAuthorization.expirationDate}".toByteArray()),
                temporaryAuthorization.signature.toSignature()
            )
        }
            .subscribeOn(Schedulers.computation())
            .map { Sha3Utils.keccak("$SIGNATURE_PREFIX${it.asEthereumAddressChecksumString()}".toByteArray()) to it }
            .flatMap { (hash, extensionAddress) ->
                (signingSafe?.let { accountsRepository.signingOwner(it) } ?: accountsRepository.createOwner())
                    .flatMap { signer -> accountsRepository.sign(signer, hash).map { signer to it } }
                    .map { it to extensionAddress }
            }
            .map { (signerAndSignature, extensionAddress) ->
                PushServicePairing(
                    ServiceSignature.fromSignature(signerAndSignature.second),
                    temporaryAuthorization = temporaryAuthorization
                ) to (signerAndSignature.first to extensionAddress)
            }
            .flatMap { pushServiceApi.pair(it.first).andThen(Single.just(it.second)) }

    override fun propagateSafeCreation(safeAddress: Solidity.Address, targets: Set<Solidity.Address>): Completable =
        Single.fromCallable { ServiceMessage.SafeCreation(safe = safeAddress.asEthereumAddressString()) }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                sendNotification(it, safeAddress, targets)
            }

    override fun propagateSubmittedTransaction(hash: String, chainHash: String, safe: Solidity.Address, targets: Set<Solidity.Address>): Completable =
        Single.fromCallable { ServiceMessage.SendTransactionHash(hash, chainHash) }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                sendNotification(it, safe, targets)
            }

    override fun propagateTransactionRejected(
        hash: String,
        signature: Signature,
        safe: Solidity.Address,
        targets: Set<Solidity.Address>
    ): Completable =
        Single.fromCallable {
            ServiceMessage.RejectTransaction(
                hash,
                signature.r.asDecimalString(),
                signature.s.asDecimalString(),
                signature.v.toInt().toString()
            )
        }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                sendNotification(it, safe, targets)
            }

    override fun requestConfirmations(
        hash: String,
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        operationalGas: BigInteger,
        gasPrice: BigInteger,
        gasToken: Solidity.Address,
        targets: Set<Solidity.Address>
    ): Completable =
        Single.fromCallable {
            ServiceMessage.RequestConfirmation(
                hash = hash,
                safe = safeAddress.asEthereumAddressChecksumString(),
                to = transaction.wrapped.address.asEthereumAddressChecksumString(),
                value = transaction.wrapped.value?.value?.asDecimalString() ?: "0",
                data = transaction.wrapped.data ?: "",
                operation = transaction.operation.toInt().toString(),
                txGas = txGas.asDecimalString(),
                dataGas = dataGas.asDecimalString(),
                operationalGas = operationalGas.asDecimalString(),
                gasPrice = gasPrice.asDecimalString(),
                gasToken = gasToken.asEthereumAddressChecksumString(),
                refundReceiver = "0",
                nonce = transaction.wrapped.nonce?.asDecimalString() ?: "0"
            )
        }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                sendNotification(it, safeAddress, targets)
            }

    override fun sendTypedDataConfirmation(
        hash: ByteArray,
        signature: ByteArray,
        safe: Solidity.Address,
        targets: Set<Solidity.Address>
    ): Completable =
        Single.fromCallable {
            ServiceMessage.TypedDataConfirmation(
                hash = hash.toHexString().addHexPrefix(),
                signature = signature.toHexString().addHexPrefix()
            )
        }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { sendNotification(it, safe, targets) }


    override fun requestTypedDataRejection(
        hash: ByteArray,
        signature: Signature,
        safe: Solidity.Address,
        targets: Set<Solidity.Address>
    ): Completable =
        Single.fromCallable {
            ServiceMessage.TypedDataRejection(
                hash = hash.toHexString().addHexPrefix(),
                r = signature.r.asDecimalString(),
                s = signature.s.asDecimalString(),
                v = signature.v.toInt().toString()
            )
        }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { sendNotification(it, safe, targets) }


    override fun requestTypedDataConfirmations(
        payload: String,
        appSignature: Signature,
        safe: Solidity.Address,
        targets: Set<Solidity.Address>
    ): Completable =
        Single.fromCallable {
            ServiceMessage.TypedDataRequest(
                payload = payload,
                safe = safe.asEthereumAddressString(),
                r = appSignature.r.asDecimalString(),
                s = appSignature.s.asDecimalString(),
                v = appSignature.v.toString()
            )
        }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable { sendNotification(it, safe, targets) }

    private fun sendNotification(message: ServiceMessage, safe: Solidity.Address, targets: Set<Solidity.Address>) =
        Single.fromCallable {
            moshi.adapter(message.javaClass).toJson(message)
        }
            .subscribeOn(Schedulers.io())
            .flatMap { rawJson ->

                Timber.d(rawJson)
                accountsRepository.sign(safe, Sha3Utils.keccak("$SIGNATURE_PREFIX$rawJson".toByteArray()))
                    .map { ServiceSignature.fromSignature(it) to rawJson }
            }
            .map { (signature, message) ->
                PushServiceNotification(
                    devices = targets.map { it.asEthereumAddressChecksumString() },
                    message = message,
                    signature = signature
                )
            }
            .flatMapCompletable { pushServiceApi.notify(it) }

    /**
     * This method is doing some blocking calculations (account recovery) and should not be used from the main thread.
     * As the name indicates it is meant for push notifications that are received in a service.
     */
    override fun handlePushMessage(pushMessage: PushMessage) {
        when (pushMessage) {
            is PushMessage.SendTransaction -> {
                // Only show notification if we have the Safe
                nullOnThrow { safeDao.querySafe(pushMessage.safe.asEthereumAddress()!!) } ?: return
                showSendTransactionNotification(pushMessage)
            }
            is PushMessage.ConfirmTransaction ->
                observedTransaction[pushMessage.hash.hexAsBigInteger()]?.publish(
                    TransactionResponse.Confirmed(
                        Signature(pushMessage.r.decimalAsBigInteger(), pushMessage.s.decimalAsBigInteger(), pushMessage.v.toInt().toByte())
                    )
                )
            is PushMessage.RejectTransaction -> {
                observedTransaction[pushMessage.hash.hexAsBigInteger()]?.publish(
                    TransactionResponse.Rejected(
                        Signature(pushMessage.r.decimalAsBigInteger(), pushMessage.s.decimalAsBigInteger(), pushMessage.v.toInt().toByte())
                    )
                )
            }
            is PushMessage.SafeCreation -> {
                pushMessage.safe.asEthereumAddress()?.let {
                    // We only want to add the safe if it was pending
                    nullOnThrow { safeDao.queryPendingSafe(it) } ?: return
                    try {
                        safeDao.pendingSafeToDeployedSafe(it)
                        showSafeCreatedNotification(it)
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
            is PushMessage.SignTypedData -> showSignTypedDataNotification(pushMessage)
            is PushMessage.SignTypedDataConfirmation -> signTypedDataSubject.onNext(pushMessage)
            is PushMessage.RejectSignTypedData -> signTypedDataSubject.onNext(pushMessage)
        }
    }

    override fun observeTypedDataPushes(): Observable<PushMessage> = signTypedDataSubject

    private fun showSafeCreatedNotification(safe: Solidity.Address) {
        localNotificationManager.show(
            safe.hashCode(),
            context.getString(R.string.safe_created_notification_title),
            context.getString(R.string.safe_created_notification_message, safe.asEthereumAddressChecksumString()),
            SafeMainActivity.createIntent(context, safe)
        )
    }

    private fun showSignTypedDataNotification(signTypedData: PushMessage.SignTypedData) {
        localNotificationManager.show(
            signTypedData.safe.hashCode(),
            context.getString(R.string.sign_message_notification_title),
            context.getString(R.string.sign_message_notification_description),

            SignatureRequestActivity.createIntent(
                context = context,
                payload = signTypedData.payload,
                safe = signTypedData.safe,
                extensionSignature = signTypedData.signature
            )
        )
    }

    private fun showSendTransactionNotification(pushMessage: PushMessage.SendTransaction) {
        pushMessage.apply {
            val transaction = Transaction(
                address = to.asEthereumAddress()!!,
                value = value.decimalAsBigIntegerOrNull()?.let { Wei(it) },
                data = data,
                nonce = nonce.decimalAsBigIntegerOrNull()
            )
            val safeTransaction = SafeTransaction(transaction, TransactionExecutionRepository.Operation.values()[operation.toInt()])
            val signature = Signature(r.decimalAsBigInteger(), s.decimalAsBigInteger(), v.toInt().toByte())
            val intent = ConfirmTransactionActivity.createIntent(
                context, signature = signature, safe = safe.asEthereumAddress()!!, transaction = safeTransaction, hash = hash,
                operationalGas = operationalGas.decimalAsBigInteger(), dataGas = dataGas.decimalAsBigInteger(), txGas = txGas.decimalAsBigInteger(),
                gasToken = gasToken.asEthereumAddress()!!, gasPrice = gasPrice.decimalAsBigInteger()
            )
            localNotificationManager.show(
                hash.hashCode(),
                context.getString(R.string.sign_transaction_request_title),
                context.getString(R.string.sign_transaction_request_message),
                intent
            )
        }
    }

    override fun calculateRejectionHash(transactionHash: ByteArray): Single<ByteArray> =
        Single.fromCallable {
            Sha3Utils.keccak("$SIGNATURE_PREFIX${transactionHash.toHexString().addHexPrefix()}${PushMessage.RejectTransaction.TYPE}".toByteArray())
        }

    override fun observe(hash: String): Observable<TransactionResponse> =
        observedTransaction.getOrPut(hash.hexAsBigInteger()) {
            ReceiveSignatureObservable {
                observedTransaction.remove(hash.hexAsBigInteger())
            }
        }.observe()

    private class ReceiveSignatureObservable(
        private val releaseCallback: (ReceiveSignatureObservable) -> Unit
    ) : ObservableOnSubscribe<TransactionResponse> {
        private val emitters = CopyOnWriteArraySet<ObservableEmitter<TransactionResponse>>()

        override fun subscribe(e: ObservableEmitter<TransactionResponse>) {
            emitters.add(e)
            e.setCancellable {
                emitters.remove(e)
                if (emitters.isEmpty()) {
                    releaseCallback.invoke(this)
                }
            }
        }

        fun observe(): Observable<TransactionResponse> = Observable.create(this)

        fun publish(signature: TransactionResponse) {
            emitters.forEach { it.onNext(signature) }
        }
    }

    companion object {
        private const val SIGNATURE_PREFIX = "GNO"
        private const val CLIENT = "android"
    }
}
