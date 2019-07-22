package pm.gnosis.heimdall.ui.messagesigning

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.awaitFirst
import kotlinx.coroutines.rx2.openSubscription
import pm.gnosis.crypto.utils.asEthereumAddressChecksumString
import pm.gnosis.eip712.*
import pm.gnosis.ethereum.EthCall
import pm.gnosis.ethereum.EthereumRepository
import pm.gnosis.heimdall.GnosisSafe
import pm.gnosis.heimdall.data.remote.models.push.PushMessage
import pm.gnosis.heimdall.data.repositories.*
import pm.gnosis.heimdall.data.repositories.models.ERC20Token
import pm.gnosis.heimdall.helpers.CryptoHelper
import pm.gnosis.heimdall.utils.getValue
import pm.gnosis.heimdall.utils.shortChecksumString
import pm.gnosis.heimdall.utils.toJson
import pm.gnosis.model.Solidity
import pm.gnosis.models.Transaction
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.utils.hexStringToByteArray
import pm.gnosis.utils.toHexString
import timber.log.Timber
import java.math.BigInteger
import javax.inject.Inject

class SignatureRequestViewModel @Inject constructor(
    private val cryptoHelper: CryptoHelper,
    private val eiP712JsonParser: EIP712JsonParser,
    private val gnosisSafeRepository: GnosisSafeRepository,
    private val encryptionManager: EncryptionManager,
    private val gnosisAccountRepository: AccountsRepository,
    private val pushServiceRepository: PushServiceRepository,
    private val addressBookRepository: AddressBookRepository,
    private val ethereumRepository: EthereumRepository,
    private val bridgeRepository: BridgeRepository
) : SignatureRequestContract() {

    private var sessionId: String? = null
    private var referenceId: Long? = null

    override val viewData: ViewData
        get() = _viewData
    private lateinit var _viewData: ViewData

    private lateinit var safe: Solidity.Address
    private lateinit var safeOwners: Set<Solidity.Address>
    private lateinit var extensionSignature: Signature
    private lateinit var payload: String
    private lateinit var domain: Struct712
    private lateinit var message: Struct712

    private lateinit var payloadEip712Hash: ByteArray
    private lateinit var safeMessageHash: ByteArray
    private lateinit var deviceSignature: Signature

    override val state: MutableLiveData<ViewUpdate> = MutableLiveData()

    private val signatures: MutableMap<Solidity.Address, Signature> = HashMap<Solidity.Address, Signature>()

    val pushChannel: ReceiveChannel<PushMessage>

    private val errorHandler = CoroutineExceptionHandler { _, e ->
        viewModelScope.launch {

        }
    }

    suspend fun handlePushMessages() = coroutineScope {

        pushChannel.consumeEach { message ->

            when (message) {
                is PushMessage.SignTypedDataConfirmation -> {
                    val signature = Signature.from(message.signature.toHexString())
                    val payloadHash = message.hash

                    val address = cryptoHelper.recover(payloadHash, signature)
                    if (safeOwners.contains(address)) {
                        Timber.d("adding signature from ${address.asEthereumAddressChecksumString()}")
                        signatures.put(address, signature)
                    }

                    _viewData = _viewData.copy(
                        status = Status.AUTHORIZATION_APPROVED
                    )

                    state.postValue(
                        ViewUpdate(
                            _viewData,
                            false,
                            null,
                            false
                        )
                    )
                }
                is PushMessage.RejectSignTypedData -> {

                    _viewData = _viewData.copy(
                        status = Status.AUTHORIZATION_REJECTED
                    )

                    state.postValue(
                        ViewUpdate(
                            _viewData,
                            false,
                            null,
                            false
                        )
                    )
                }
                else -> {
                    // handling only sign typed data pushes
                }
            }
        }
    }

    init {
        pushChannel = pushServiceRepository.observeTypedDataPushes().openSubscription()
        viewModelScope.launch(Dispatchers.IO) {
            handlePushMessages()
        }
    }


    override fun setup(payload: String, safe: Solidity.Address, extensionSignature: Signature?, referenceId: Long?, sessionId: String?) {

        viewModelScope.launch(Dispatchers.IO) {

            this@SignatureRequestViewModel.referenceId = referenceId
            this@SignatureRequestViewModel.sessionId = sessionId
            this@SignatureRequestViewModel.safe = safe
            this@SignatureRequestViewModel.payload = payload

            val domainWithMessage = eiP712JsonParser.parseMessage(payload) ?: throw InvalidPayload

            domain = domainWithMessage.domain
            message = domainWithMessage.message

            val (safeName, safeAddress) = async {
                addressBookRepository.observeAddressBookEntry(safe)
                    .map { it.name to it.address.shortChecksumString() }
                    .awaitFirst()
            }.await()

            val safeInfo = async {
                gnosisSafeRepository.loadInfo(safe).awaitFirst()
            }.await()

            val deviceOwner = async { gnosisAccountRepository.signingOwner(safe).await() }.await()
            safeOwners = safeInfo.owners.toSet().minus(deviceOwner.address)

            val safeBalance = ERC20Token.ETHER_TOKEN.displayString(safeInfo.balance.value)

            val dappName = domain.parameters.find { it.name == "name" }?.getValue() as String
            val dappAddress = domain.parameters.find { it.name == "verifyingContract" }?.getValue() as Solidity.Address

            payloadEip712Hash = typedDataHash(message = message, domain = domain)

            val safeMessageStruct = Struct712(
                typeName = "SafeMessage",
                parameters = listOf(
                    Struct712Parameter(
                        name = "message",
                        type = Literal712(typeName = "bytes", value = Solidity.Bytes(payloadEip712Hash))
                    )
                )
            )

            val safeDomain = Struct712(
                typeName = "EIP712Domain",
                parameters = listOf(
                    Struct712Parameter(
                        name = "verifyingContract",
                        type = Literal712("address", safe)
                    )
                )
            )

            safeMessageHash = typedDataHash(message = safeMessageStruct, domain = safeDomain)

            deviceSignature = cryptoHelper.sign(deviceOwner.privateKey.value(encryptionManager), safeMessageHash)
            // async { gnosisSafeRepository.sign(safe, safeMessageHash).await() }.await()
            signatures.put(deviceOwner.address, deviceSignature)

            Timber.d("safe owners ${safeInfo.owners.map { it.asEthereumAddressChecksumString() }}")
            Timber.d("device owner" + deviceOwner.address.asEthereumAddressChecksumString())

            if (extensionSignature == null) {

                _viewData = ViewData(
                    safeAddress = safe,
                    safeName = safeName,
                    safeBalance = safeBalance,
                    domainPayload = domain.toJson("root"),
                    messagePayload = message.toJson("root"),
                    dappAddress = dappAddress,
                    dappName = dappName,
                    status = Status.AUTHORIZATION_REQUIRED
                )

                try {
                    pushServiceRepository.requestTypedDataConfirmations(
                        payload,
                        deviceSignature,
                        safe,
                        safeOwners
                    )
                        .subscribeOn(Schedulers.io())
                        .await()

                    state.postValue(
                        ViewUpdate(
                            _viewData,
                            false,
                            null,
                            false
                        )
                    )

                } catch (e: Exception) {

                    Timber.e(e)

                    state.postValue(
                        ViewUpdate(
                            _viewData,
                            false,
                            ErrorSendingPush,
                            false
                        )
                    )

                }

            } else {

                this@SignatureRequestViewModel.extensionSignature = extensionSignature

                _viewData = ViewData(
                    safeAddress = safe,
                    safeName = safeName,
                    safeBalance = safeBalance,
                    domainPayload = domain.toJson("root"),
                    messagePayload = message.toJson("root"),
                    dappAddress = dappAddress,
                    dappName = dappName,
                    status = Status.READY_TO_SIGN
                )

                state.postValue(
                    ViewUpdate(
                        _viewData,
                        false,
                        null,
                        false
                    )
                )

            }
        }
    }

    override fun resend() {
        viewModelScope.launch(Dispatchers.IO) {

            try {
                pushServiceRepository.requestTypedDataConfirmations(
                    payload,
                    deviceSignature,
                    safe,
                    safeOwners
                )
                    .subscribeOn(Schedulers.io())
                    .await()

            } catch (e: Exception) {

                Timber.e(e)

                state.postValue(
                    ViewUpdate(
                        _viewData,
                        false,
                        ErrorSendingPush,
                        false
                    )
                )
            }
        }
    }

    override fun sign() {

        viewModelScope.launch(Dispatchers.IO) {

            val finalSignature = signatures.map {
                it.value.toString().hexStringToByteArray()
            }
                .sortedBy { BigInteger(it) }
                .reduce { acc, bytes ->
                    acc + bytes
                }

            Timber.d("safe message hash: ${safeMessageHash.toHexString()}")
            Timber.d("final signature: ${finalSignature.toHexString()}")

            bridgeRepository.approveRequest(referenceId!!, finalSignature.toHexString()).await()

            //FIXME: payloadEip712Hash vs safeMessageHash?
            val data = GnosisSafe.IsValidSignature.encode(Solidity.Bytes(payloadEip712Hash), Solidity.Bytes(finalSignature))

            try {

                val result = ethereumRepository.request(EthCall(safe, Transaction(address = safe, data = data)))
                    .subscribeOn(Schedulers.io())
                    .awaitFirst()

                if (result.result() == GnosisSafe.IsValidSignature.METHOD_ID) {
                    Timber.d("valid signature")
                } else {
                    Timber.d("invalid signature")
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            state.postValue(
                ViewUpdate(
                    _viewData,
                    false,
                    null,
                    true
                )
            )
        }
    }

    override fun confirmPayload() {

        viewModelScope.launch(Dispatchers.IO) {

            state.postValue(
                ViewUpdate(
                    _viewData,
                    true,
                    null,
                    false
                )
            )

            val requester = try {
                cryptoHelper.recover(safeMessageHash, extensionSignature)
            } catch (e: Exception) {
                throw ErrorRecoveringSender
            }

            try {
                pushServiceRepository.sendTypedDataConfirmation(
                    hash = safeMessageHash,
                    safe = safe,
                    signature = deviceSignature.toString().hexStringToByteArray(),
                    targets = setOf(requester)
                )
                    .subscribeOn(Schedulers.io())
                    .await()

                state.postValue(
                    ViewUpdate(
                        _viewData,
                        false,
                        null,
                        true
                    )
                )

            } catch (e: Exception) {

                state.postValue(
                    ViewUpdate(
                        _viewData,
                        false,
                        ErrorSendingPush,
                        false
                    )
                )
            }
        }
    }

    override fun cancel() {
        viewModelScope.launch {
            try {
                pushServiceRepository.requestTypedDataRejection(
                    safeMessageHash,
                    deviceSignature,
                    safe,
                    safeOwners
                )
                    .subscribeOn(Schedulers.io())
                    .await()
            } catch (e: Exception) {
                Timber.e(e)
            }

            try {
                bridgeRepository.rejectRequest(referenceId!!, 104, "Rejected").await()
            } catch (e: Exception) {
                // session already closed
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pushChannel.cancel()
    }
}





