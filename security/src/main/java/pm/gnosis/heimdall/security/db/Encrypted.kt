package pm.gnosis.heimdall.security.db

import android.arch.persistence.room.TypeConverter
import pm.gnosis.heimdall.security.EncryptionManager
import pm.gnosis.utils.utf8String

private interface Encrypted<out T> {
    fun value(encryptionManager: EncryptionManager): T

    interface Creator<T> {
        fun create(encryptionManager: EncryptionManager, value: T): Encrypted<T>
    }

    interface Converter<W : Encrypted<Any>> {
        fun toStorage(wrapper: W): String
        fun fromStorage(value: String): W
    }
}

class EncryptedByteArray private constructor(private val encryptedValue: String) : Encrypted<ByteArray> {

    override fun value(encryptionManager: EncryptionManager): ByteArray {
        return encryptionManager.decrypt(EncryptionManager.CryptoData.fromString(encryptedValue))
    }

    companion object : Encrypted.Creator<ByteArray> {
        override fun create(encryptionManager: EncryptionManager, value: ByteArray): EncryptedByteArray {
            return EncryptedByteArray(encryptionManager.encrypt(value).toString())
        }
    }

    class Converter : Encrypted.Converter<EncryptedByteArray> {
        @TypeConverter
        override fun toStorage(wrapper: EncryptedByteArray): String {
            return wrapper.encryptedValue
        }

        @TypeConverter
        override fun fromStorage(value: String): EncryptedByteArray {
            return EncryptedByteArray(value)
        }
    }
}

class EncryptedString private constructor(private val encryptedValue: String) : Encrypted<String> {

    override fun value(encryptionManager: EncryptionManager): String {
        return encryptionManager.decrypt(EncryptionManager.CryptoData.fromString(encryptedValue)).utf8String()
    }

    companion object : Encrypted.Creator<String> {
        override fun create(encryptionManager: EncryptionManager, value: String): EncryptedString {
            return EncryptedString(encryptionManager.encrypt(value.toByteArray()).toString())
        }
    }

    class Converter : Encrypted.Converter<EncryptedString> {
        @TypeConverter
        override fun toStorage(wrapper: EncryptedString): String {
            return wrapper.encryptedValue
        }

        @TypeConverter
        override fun fromStorage(value: String): EncryptedString {
            return EncryptedString(value)
        }
    }
}