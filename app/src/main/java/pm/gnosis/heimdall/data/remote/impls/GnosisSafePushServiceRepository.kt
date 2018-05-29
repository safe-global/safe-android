package pm.gnosis.heimdall.data.remote.impls

import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.heimdall.data.remote.GnosisSafePushService
import pm.gnosis.heimdall.data.remote.PushServiceRepository
import pm.gnosis.heimdall.data.remote.models.push.*
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Account
import pm.gnosis.svalinn.accounts.base.repositories.AccountsRepository
import pm.gnosis.svalinn.common.PreferencesManager
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.utils.asEthereumAddressString
import timber.log.Timber
import javax.inject.Inject

class GnosisSafePushServiceRepository @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val preferencesManager: PreferencesManager,
    private val gnosisSafePushService: GnosisSafePushService,
    private val moshi: Moshi
) : PushServiceRepository {

    private val disposables = CompositeDisposable()

    /*
    * Situations where a sync might be needed:
    * • Account changes
    * • Token was not synced to Gnosis Safe Push Notification Service (eg.: no Internet connection, service down)
    *
    * Warning: This call might fail if the device is not unlocked (no access to ethereum account)
    */
    override fun syncAuthentication(forced: Boolean) {
        disposables += accountsRepository.loadActiveAccount()
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

    override fun pair(temporaryAuthorization: PushServiceTemporaryAuthorization): Single<Solidity.Address> =
        accountsRepository.recover(
            Sha3Utils.keccak("$SIGNATURE_PREFIX${temporaryAuthorization.expirationDate}".toByteArray()),
            temporaryAuthorization.signature.toSignature()
        )
            .map { Sha3Utils.keccak("$SIGNATURE_PREFIX${it.asEthereumAddressChecksumString()}".toByteArray()) to it }
            .flatMap { (hash, extensionAddress) -> accountsRepository.sign(hash).map { it to extensionAddress } }
            .map { (signature, extensionAddress) ->
                PushServicePairing(
                    PushServiceSignature.fromSignature(signature),
                    temporaryAuthorization = temporaryAuthorization
                ) to extensionAddress
            }
            .flatMap { gnosisSafePushService.pair(it.first).andThen(Single.just(it.second)) }

    override fun sendSafeCreationNotification(safeAddress: Solidity.Address, devicesToNotify: Set<Solidity.Address>): Completable =
        Single.fromCallable { SafeCreationParams(safe = safeAddress.asEthereumAddressString()) }
            .flatMap { pushMessage ->
                val rawJson = moshi.adapter(SafeCreationParams::class.java).toJson(pushMessage)
                accountsRepository.sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$rawJson".toByteArray()))
                    .map { PushServiceSignature.fromSignature(it) to rawJson }
            }
            .map {
                PushServiceNotification(
                    devices = devicesToNotify.map { it.asEthereumAddressChecksumString() },
                    message = it.second,
                    signature = it.first
                )
            }
            .flatMapCompletable { gnosisSafePushService.notify(it) }


    private fun syncAuthentication(account: Account, pushToken: String) =
        accountsRepository.sign(Sha3Utils.keccak("$SIGNATURE_PREFIX$pushToken".toByteArray()))
            .map { PushServiceAuth(pushToken, PushServiceSignature.fromSignature(it)) }
            .flatMapCompletable { gnosisSafePushService.auth(it) }
            .doOnComplete {
                preferencesManager.prefs.edit {
                    putString(LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY, bundleAccountWithPushToken(account, pushToken))
                }
            }

    private fun bundleAccountWithPushToken(account: Account, pushToken: String) = "${account.address.asEthereumAddressString()}$pushToken"


    companion object {
        const val LAST_SYNC_ACCOUNT_AND_TOKEN_PREFS_KEY = "prefs.string.accounttoken"
        const val SIGNATURE_PREFIX = "GNO"
    }
}
