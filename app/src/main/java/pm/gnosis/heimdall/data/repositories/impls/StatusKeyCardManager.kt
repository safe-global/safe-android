package pm.gnosis.heimdall.data.repositories.impls

import im.status.keycard.applet.*
import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.heimdall.data.repositories.CardRepository
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.toHex
import pm.gnosis.utils.toHexString

class StatusKeyCardManager(private val cmdSet: KeycardCommandSet) : CardRepository.CardManager {

    private var info: ApplicationInfo? = null

    override fun status() = info?.toStatus()

    override fun start(): CardRepository.CardManager.CardStatus =
        ApplicationInfo(cmdSet.select().checkOK().data).let {
            info = it
            it.toStatus()
        }

    override fun init(params: CardRepository.CardManager.InitParams) {
        require(params is InitParams) { "Invalid init params" }
        cmdSet.init(params.pin, params.puk, params.pairingKey).checkOK()
    }

    override fun unlock(params: CardRepository.CardManager.UnlockParams) {
        val pin = when (params) {
            is UnlockParams -> params.pin
            is PairingParams -> params.pin
            else -> throw IllegalArgumentException("Invalid init params")
        }
        cmdSet.verifyPIN(pin).checkAuthOK()
    }

    override fun pair(params: CardRepository.CardManager.PairingParams): String {
        require(params is PairingParams) { "Invalid init params" }
        val info = info ?: throw IllegalStateException("Manager not started")
        if (info.hasSecureChannelCapability()) {
            cmdSet.autoPair(params.pairingKey)
            cmdSet.autoOpenSecureChannel()
            return cmdSet.pairing.toBase64()
        }
        return ""
    }

    override fun pair(sessionKey: String) {
        val info = info ?: throw IllegalStateException("Manager not started")
        if (info.hasSecureChannelCapability()) {
            cmdSet.pairing = Pairing(sessionKey)
            cmdSet.autoOpenSecureChannel()
        }
    }

    override fun setupCrypto() {
        val info = info ?: throw IllegalStateException("Manager not started")
        if (!info.hasMasterKey() && info.hasKeyManagementCapability()) {
            cmdSet.generateKey()
        }

        cmdSet.setNDEF(byteArrayOf())
    }

    override fun sign(hash: ByteArray, keyPath: String): Pair<Solidity.Address, ECDSASignature> {
        cmdSet.deriveKey(keyPath).checkOK() // TODO: check why it is not working with signWithPath
        val signature = RecoverableSignature(hash, cmdSet.sign(hash).checkOK().data)
        val walletPublicKey = BIP32KeyPair(null, null, signature.publicKey)
        return walletPublicKey.toEthereumAddress().toHexString().asEthereumAddress()!! to
                signature.run { ECDSASignature(r.asBigInteger(), s.asBigInteger()).apply { v = (recId + 27).toByte() } }
    }

    override fun clear(): Boolean {
        val unpair = cmdSet.pairing != null
        if (unpair) cmdSet.autoUnpair()
        return unpair
    }

    private fun ApplicationInfo.toStatus() =
        when (isInitializedCard) {
            true -> CardRepository.CardManager.CardStatus.Initialized(id())
            false -> CardRepository.CardManager.CardStatus.Uninitialized
        }


    private fun ApplicationInfo.id() = instanceUID.toHex()

    data class InitParams(val pin: String, val puk: String, val pairingKey: String) :
        CardRepository.CardManager.InitParams
    data class UnlockParams(val pin: String) : CardRepository.CardManager.UnlockParams
    data class PairingParams(val pin: String, val pairingKey: String) : CardRepository.CardManager.PairingParams,
        CardRepository.CardManager.UnlockParams

}