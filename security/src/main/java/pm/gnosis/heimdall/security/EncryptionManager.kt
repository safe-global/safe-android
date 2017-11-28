package pm.gnosis.heimdall.security

import io.reactivex.Single


interface EncryptionManager {
    fun decrypt(data: ByteArray): ByteArray
    fun encrypt(data: ByteArray): ByteArray
    fun unlocked(): Single<Boolean>
    fun unlockWithPassword(key: ByteArray): Single<Boolean>
    fun lock()
    fun setupPassword(newKey: ByteArray, oldKey: ByteArray? = null): Single<Boolean>
    fun initialized(): Single<Boolean>
}