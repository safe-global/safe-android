package io.gnosis.safe.utils

import com.keystone.module.HDKey
import com.keystone.module.Note
import com.keystone.sdk.KeystoneSDK
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asBigInteger
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger

interface PublicAddressDerivator: AddressPagingSource {
    fun initialize(hdKey: HDKey)
    fun addressesForRange(range: LongRange): List<Solidity.Address>
}

interface PublicKeyDerivator {
    fun initialize(hdKey: HDKey)
    fun keyForIndex(index: Long): BigInteger
}

class PublicKeyAndAddressDerivator: PublicKeyDerivator, PublicAddressDerivator {

    private lateinit var hdKey: HDKey
    private val sdk = KeystoneSDK()

    override fun initialize(hdKey: HDKey) {
        this.hdKey = hdKey
    }

    override fun addressesForRange(range: LongRange): List<Solidity.Address> {
        val result = mutableListOf<Solidity.Address>()
        range.map {
            val derivedKey = derivedKeyForIndex(it)
            val keyPair = KeyPair.fromPublicOnly(derivedKey)
            result.add(Solidity.Address(keyPair.address.asBigInteger()))
        }
        return result
    }

    override fun addressesForPage(start: Long, pageSize: Int): List<Solidity.Address> {
        return addressesForRange(LongRange(start, start + pageSize - 1))
    }

    override fun keyForIndex(index: Long): BigInteger {
        return derivedKeyForIndex(index).asBigInteger()
    }

    private fun derivedKeyForIndex(index: Long): ByteArray {
        val path = if (hdKey.note == Note.LEDGER_LEGACY.value) "m/$index" else "m/0/$index"
        return sdk.derivePublicKey(hdKey.xpub, path).hexStringToByteArray()
    }
}
