package pm.gnosis.heimdall.data.repositories.impls

import android.content.Context
import android.content.Intent
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.moshi.Moshi
import io.reactivex.*
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.data.remote.PushServiceApi
import pm.gnosis.heimdall.data.remote.models.push.*
import pm.gnosis.heimdall.data.repositories.PushServiceRepository
import pm.gnosis.heimdall.data.repositories.PushServiceRepository.TransactionResponse
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository
import pm.gnosis.heimdall.data.repositories.TransactionInfoRepository
import pm.gnosis.heimdall.data.repositories.models.SafeTransaction
import pm.gnosis.heimdall.data.repositories.toInt
import pm.gnosis.heimdall.di.ApplicationContext
import pm.gnosis.heimdall.helpers.LocalNotificationManager
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.models.Wei
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.*
import timber.log.Timber
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject

class DefaultPushServiceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountsRepository: AccountsRepository,
    private val notificationManager: LocalNotificationManager,
    private val moshi: Moshi,
    private val preferencesManager: PreferencesManager,
    private val pushServiceApi: PushServiceApi
) : PushServiceRepository {


    private val observedTransaction = HashMap<BigInteger, ReceiveSignatureObservable>()

    /*
    * Situations where a sync might be needed:
    * • Account changes
    * • Token was not synced to Gnosis Safe Push Notification Service (eg.: no Internet connection, service down)
    *
    * Warning: This call might fail if the device is not unlocked (no access to ethereum account)
    */
    override fun syncAuthentication(forced: Boolean) {
        accountsRepository.loadActiveAccount()
            .map { account ->
                val currentToken = FirebaseInstanceId.getInstance().token ?: throw IllegalStateException("Firebase token is null")
                val lastSyncedData = preferencesManager.prefs.getString(LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY, "")
                val currentData = bundleAccountWithPushToken(account, currentToken)
                (lastSyncedData != currentData) to (account to currentToken)
            }
            .flatMapCompletable { (needsSync, accountTokenPair) ->
                if (needsSync || forced) syncAuthentication(accountTokenPair.first, accountTokenPair.second)
                else Completable.complete()
            }
            .subscribeBy(onComplete = { Timber.d("GnosisSafePushServiceRepository: successful sync") }, onError = Timber::e)
    }


    private fun syncAuthentication(account: Account, pushToken: String) =
        accountsRepository.sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$pushToken".toByteArray()))
            .map { PushServiceAuth(pushToken, ServiceSignature.fromSignature(it)) }
            .flatMapCompletable { pushServiceApi.auth(it) }
            .doOnComplete {
                preferencesManager.prefs.edit {
                    putString(LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY, bundleAccountWithPushToken(account, pushToken))
                }
            }

    private fun bundleAccountWithPushToken(account: Account, pushToken: String) = "${account.address.asEthereumAddressString()}$pushToken"

    override fun pair(temporaryAuthorization: PushServiceTemporaryAuthorization): Single<Solidity.Address> =
        accountsRepository.recover(
            Sha3Utils.keccak("$SIGNATURE_PREFIX${temporaryAuthorization.expirationDate}".toByteArray()),
            temporaryAuthorization.signature.toSignature()
        )
            .map { Sha3Utils.keccak("$SIGNATURE_PREFIX${it.asEthereumAddressChecksumString()}".toByteArray()) to it }
            .flatMap { (hash, extensionAddress) -> accountsRepository.sign(hash).map { it to extensionAddress } }
            .map { (signature, extensionAddress) ->
                PushServicePairing(
                    ServiceSignature.fromSignature(signature),
                    temporaryAuthorization = temporaryAuthorization
                ) to extensionAddress
            }
            .flatMap { pushServiceApi.pair(it.first).andThen(Single.just(it.second)) }

    override fun propagateSafeCreation(safeAddress: Solidity.Address, targets: Set<Solidity.Address>): Completable =
        Single.fromCallable { ServiceMessage.SafeCreation(safe = safeAddress.asEthereumAddressString()) }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                sendNotification(it, targets)
            }

    override fun requestConfirmations(
        hash: String,
        safeAddress: Solidity.Address,
        transaction: SafeTransaction,
        txGas: BigInteger,
        dataGas: BigInteger,
        gasPrice: BigInteger,
        targets: Set<Solidity.Address>
    ): Completable =
        Single.fromCallable {
            ServiceMessage.RequestConfirmation(
                hash = hash,
                safe = safeAddress.asEthereumAddressString(),
                to = transaction.wrapped.address.asEthereumAddressChecksumString(),
                value = transaction.wrapped.value?.value?.asDecimalString() ?: "0",
                data = transaction.wrapped.data ?: "",
                operation = transaction.operation.toInt().toString(),
                txGas = txGas.asDecimalString(),
                dataGas = dataGas.asDecimalString(),
                gasPrice = gasPrice.asDecimalString(),
                gasToken = "0",
                nonce = transaction.wrapped.nonce?.asDecimalString() ?: "0"
            )
        }
            .subscribeOn(Schedulers.io())
            .flatMapCompletable {
                sendNotification(it, targets)
            }

    private fun sendNotification(message: ServiceMessage, targets: Set<Solidity.Address>) =
        Single.fromCallable {
            moshi.adapter(message.javaClass).toJson(message)
        }
            .subscribeOn(Schedulers.io())
            .flatMap { rawJson ->
                accountsRepository.sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$rawJson".toByteArray()))
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
            is PushMessage.SendTransaction ->
                showSendTransactionNotification(pushMessage)
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
        }
    }

    private fun showSendTransactionNotification(pushMessage: PushMessage.SendTransaction) {
        val transaction = Transaction(
            address = pushMessage.to.asEthereumAddress()!!,
            value = pushMessage.value.decimalAsBigIntegerOrNull()?.let { Wei(it) },
            data = pushMessage.data,
            nonce = pushMessage.nonce.decimalAsBigIntegerOrNull()
        )
        val safeTransaction = SafeTransaction(transaction, TransactionExecutionRepository.Operation.values()[pushMessage.operation.toInt()])
        // TODO: create confirm activity
        val intent = Intent()
        notificationManager.show(
            pushMessage.hash.hashCode(),
            context.getString(R.string.sign_transaction_request_title),
            context.getString(R.string.sign_transaction_request_message),
            intent
        )
    }

    override fun calculateRejectionHash(transactionHash: ByteArray): Single<ByteArray> =
        Single.fromCallable {
            Sha3Utils.keccak("$SIGNATURE_PREFIX${transactionHash.toHexString().addHexPrefix()}${PushMessage.RejectTransaction.TYPE}".toByteArray())
        }

    override fun observe(hash: String): Observable<TransactionResponse> =
        observedTransaction.getOrPut(hash.hexAsBigInteger(), {
            ReceiveSignatureObservable({
                observedTransaction.remove(hash.hexAsBigInteger())
            })
        }).observe()

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
        private const val LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY = "prefs.string.accounttoken"
        private const val SIGNATURE_PREFIX = "GNO"
    }
}
