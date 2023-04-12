package io.gnosis.safe.utils

import okio.ByteString
import pm.gnosis.crypto.HDNode
import pm.gnosis.crypto.KeyGenerator
import pm.gnosis.mnemonic.Bip39
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asBigInteger
import java.math.BigInteger

interface AddressPagingSource {
    fun addressesForPage(start: Long, pageSize: Int): List<Solidity.Address>
}

interface MnemonicAddressDerivator: AddressPagingSource {
    fun initialize(mnemonic: String)
    fun addressesForRange(range: LongRange): List<Solidity.Address>
}

interface MnemonicKeyDerivator {
    fun initialize(mnemonic: String)
    fun keyForIndex(index: Long): BigInteger
}

class MnemonicKeyAndAddressDerivator(
        private val bip39: Bip39
) : MnemonicKeyDerivator, MnemonicAddressDerivator {

    private lateinit var masterKey: HDNode

    override fun initialize(mnemonic: String) {
        val mnemonicSeed = bip39.mnemonicToSeed(mnemonic)
        val hdNode = KeyGenerator.masterNode(ByteString.of(*mnemonicSeed))
        masterKey = hdNode.derive(KeyGenerator.BIP44_PATH_ETHEREUM)
    }

    override fun addressesForRange(range: LongRange): List<Solidity.Address> {
        val result = mutableListOf<Solidity.Address>()
        range.map {
            val key = masterKey.deriveChild(it).keyPair
            val address = key.address.asBigInteger()
            result.add(Solidity.Address(address))
        }
        return result
    }

    override fun addressesForPage(start: Long, pageSize: Int): List<Solidity.Address> {
        return addressesForRange(LongRange(start, start + pageSize - 1))
    }

    override fun keyForIndex(index: Long): BigInteger {
        val keyPair = masterKey.deriveChild(index).keyPair
        return keyPair.privKey
    }

}
