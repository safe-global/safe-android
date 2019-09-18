package pm.gnosis.heimdall.data.repositories.impls

import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.data.repositories.CardRepository
import pm.gnosis.heimdall.data.repositories.CardRepository.CardManager.CardStatus
import pm.gnosis.heimdall.helpers.AppPreferencesManager
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.common.utils.edit
import pm.gnosis.svalinn.security.EncryptionManager
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.utf8String
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCardRepository @Inject constructor(
    private val encryptionManager: EncryptionManager,
    appPreferencesManager: AppPreferencesManager
) : CardRepository {

    private val pairingPreferences = appPreferencesManager.get(PREFERENCES_PAIRING_INFO)

    override suspend fun loadKeyCards(): List<CardRepository.CardInfo> =
        pairingPreferences.all.mapNotNull {
            (it.value as? String)?.let { encoded -> decodeCardInfo(it.key, encoded) }
        }

    private fun decodeCardInfo(id: String, encoded: String) =
        encoded.split(";").let {
            val pairing = it.getOrNull(0)?.let { crypted ->
                encryptionManager.decrypt(EncryptionManager.CryptoData.fromString(crypted)).utf8String()
            } ?: return@let null
            val name = it.getOrNull(1) ?: "Unknown card"
            val date = it.getOrNull(2)?.toLongOrNull() ?: 0
            CardRepository.CardInfo(id, name, date, pairing)
        }

    override suspend fun loadKeyCard(id: String): CardRepository.CardInfo =
        getCardInfo(id) ?: throw NoSuchElementException()

    private fun getCardInfo(id: String) =
        pairingPreferences.getString(id, null)?.let { decodeCardInfo(id, it) }

    private fun addCardInfo(id: String, pairing: String, label: String) {
        val encryptedPairing = encryptionManager.encrypt(pairing.toByteArray()).toString()
        val cleanLabel = label.replace(";", " ")
        pairingPreferences.edit { putString(id, "$encryptedPairing;$cleanLabel;${System.currentTimeMillis()}") }
    }

    private fun removeCardInfo(id: String) =
        pairingPreferences.edit { remove(id) }

    override suspend fun initCard(manager: CardRepository.CardManager, initParams: CardRepository.CardManager.InitParams) {
        val status = manager.start()
        check(status is CardStatus.Uninitialized) { "Card already initialized" }
        manager.init(initParams)
    }

    override suspend fun pairCard(
        manager: CardRepository.CardManager,
        pairingParams: CardRepository.CardManager.PairingParams,
        label: String,
        keyIndex: Long
    ): Solidity.Address {
        try {
            val status = manager.start()
            check(status is CardStatus.Initialized) { "Card not initialized" }
            print(status)
            when (val info = getCardInfo(status.id)) {
                null -> {
                    addCardInfo(status.id, manager.pair(pairingParams), label)
                }
                else -> manager.pair(info.sessionKey)
            }
            print(pairingParams)
            manager.unlock(pairingParams)
            manager.setupCrypto()
            val hash = Sha3Utils.keccak("Gnosis".toByteArray())
            val path = HD_BASE_PATH + keyIndex.toString(10)
            val (walletAddress, signature) = manager.sign(hash, path)

            KeyPair.recoverFromSignature(signature.v.toInt() - 27, signature, hash)?.address?.asBigInteger()?.let {
                check(Solidity.Address(it) == walletAddress) { "Illegal card address" }
            } ?: throw IllegalStateException("Could not recover signature")

            return walletAddress
        } catch (e: Exception) {
            // In case of an error clear up all stored info
            if (manager.clear()) {
                (manager.status() as? CardStatus.Initialized)?.let { removeCardInfo(it.id) }
            }
            throw e
        }
    }

    override suspend fun signWithCard(
        manager: CardRepository.CardManager,
        unlockParams: CardRepository.CardManager.UnlockParams,
        hash: ByteArray,
        keyIndex: Long
    ): Pair<Solidity.Address, ECDSASignature> {
        val status = manager.start()
        check(status is CardStatus.Initialized) { "Card not initialized" }

        val cardInfo = getCardInfo(status.id) ?: throw IllegalStateException("Unknown card")
        manager.pair(cardInfo.sessionKey)

        // Unlock
        manager.unlock(unlockParams)

        // Sign
        val path = HD_BASE_PATH + keyIndex.toString(10)
        return manager.sign(hash, path)
    }

    companion object {
        private const val PREFERENCES_PAIRING_INFO = "preferences.status_key_card_repository.pairing_info"
        private const val HD_BASE_PATH = "m/44'/60'/0'/0/"
    }

}