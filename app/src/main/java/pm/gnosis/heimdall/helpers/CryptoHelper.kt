package pm.gnosis.heimdall.helpers

import io.reactivex.Single
import pm.gnosis.crypto.KeyPair
import pm.gnosis.model.Solidity
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.asBigInteger
import javax.inject.Inject

interface CryptoHelper {
    fun recover(hash: ByteArray, signature: Signature): Solidity.Address

    fun sign(privateKey: ByteArray, hash: ByteArray): Signature
}

class SvalinnCryptoHelper @Inject constructor() : CryptoHelper {
    override fun recover(hash: ByteArray, signature: Signature): Solidity.Address =
        Solidity.Address(KeyPair.signatureToKey(hash, signature.v, signature.r, signature.s).address.asBigInteger())

    override fun sign(privateKey: ByteArray, hash: ByteArray): Signature =
        KeyPair.fromPrivate(privateKey).sign(hash).let { Signature(it.r, it.s, it.v) }
}

interface SignerWrapper {
    fun sign(hash: ByteArray): Single<Signature>
}