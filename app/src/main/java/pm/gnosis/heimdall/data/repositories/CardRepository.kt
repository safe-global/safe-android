package pm.gnosis.heimdall.data.repositories

import pm.gnosis.crypto.ECDSASignature
import pm.gnosis.model.Solidity
import java.math.BigInteger

interface CardRepository {

    suspend fun loadKeyCards(): List<CardInfo>

    suspend fun loadKeyCard(id: String): CardInfo

    suspend fun initCard(manager: CardManager, initParams: CardManager.InitParams)

    suspend fun pairCard(
        manager: CardManager,
        pairingParams: CardManager.PairingParams,
        label: String,
        keyIndex: Long = 0L
    ): Solidity.Address

    suspend fun signWithCard(
        manager: CardManager,
        unlockParams: CardManager.UnlockParams,
        hash: ByteArray,
        keyIndex: Long = 0L
    ): Pair<Solidity.Address, ECDSASignature>

    data class CardInfo(val cardId: String, val label: String, val paired: Long, val sessionKey: String)

    interface CardManager {

        fun status(): CardStatus?

        fun start(): CardStatus

        fun init(params: InitParams)

        fun unlock(params: UnlockParams)

        // Returns a sessionKey that can be used later
        fun pair(params: PairingParams): String

        fun pair(sessionKey: String)

        fun setupCrypto()

        fun sign(hash: ByteArray, keyPath: String): Pair<Solidity.Address, ECDSASignature>

        fun clear(): Boolean

        sealed class CardStatus {
            object Uninitialized: CardStatus()
            data class Initialized(val id: String): CardStatus()
        }

        interface InitParams
        interface UnlockParams
        interface PairingParams : UnlockParams
    }
}