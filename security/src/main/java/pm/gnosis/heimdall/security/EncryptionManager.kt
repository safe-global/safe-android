package pm.gnosis.heimdall.security

import io.reactivex.Single


interface EncryptionManager {
    fun decrypt(data: ByteArray): ByteArray
    fun encrypt(data: ByteArray): ByteArray
    fun unlocked(): Single<Boolean>
    fun unlock(key: ByteArray): Single<Boolean>
    fun lock()
    fun setup(newKey: ByteArray, oldKey: ByteArray? = null): Single<Boolean>
    fun initialized(): Single<Boolean>
}