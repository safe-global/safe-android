package pm.gnosis.crypto

import org.junit.Assert.*
import org.junit.Test
import org.spongycastle.util.encoders.Base64
import pm.gnosis.crypto.exceptions.MissingPrivateKeyException
import pm.gnosis.tests.utils.Asserts.assertThrow
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexStringToByteArray
import java.math.BigInteger

class KeyPairTest {

    private fun checkKeyPair(keyPair: KeyPair) {
        assertArrayEquals("02e3c64e2b1d86045297da6167c1b6f26bbdb0ab572728bf3d6722f87e5f0a4838".hexStringToByteArray(), keyPair.pubKey)
        assertArrayEquals("5E99EA7471342B32ee0D0dfa3C9B4a8DCE453E57".hexStringToByteArray(), keyPair.address)
        assertArrayEquals("75205699e5fab1e0fa5c3d41898d00f8d251dc38f09ab85b9f5f035f72725310".hexStringToByteArray(), keyPair.privKeyBytes)
        assertEquals(BigInteger("75205699e5fab1e0fa5c3d41898d00f8d251dc38f09ab85b9f5f035f72725310", 16), keyPair.privKey)
        assertFalse(keyPair.isPubKeyOnly)
        assertTrue(keyPair.isPubKeyCanonical)
        assertEquals("pub:02e3c64e2b1d86045297da6167c1b6f26bbdb0ab572728bf3d6722f87e5f0a4838", keyPair.toString())
        assertEquals("pub:02e3c64e2b1d86045297da6167c1b6f26bbdb0ab572728bf3d6722f87e5f0a4838 priv:75205699e5fab1e0fa5c3d41898d00f8d251dc38f09ab85b9f5f035f72725310", keyPair.toStringWithPrivate())
    }

    @Test
    fun failures() {
        val data = "9b8bc77908c0b0ebe93e897e43f594b811f5d7130d86a5708403ddb417dc111b".hexStringToByteArray()
        assertThrow({ KeyPair.signatureToKey(data, "NotBase64Hash;)") })
        assertThrow({ KeyPair.signatureToKey(data, Base64.toBase64String(byteArrayOf(0x04, 0x02))) })

        val signature = ECDSASignature(
                "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexAsBigInteger(),
                "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexAsBigInteger()).apply { v = 27 }
        assertThrow({ KeyPair.signatureToKey(data, 23.toByte(), signature.r, signature.s) })
        assertThrow({ KeyPair.signatureToKey(data, 42.toByte(), signature.r, signature.s) })
    }

    @Test
    fun getPrivKey() {
        val fromByteArray = KeyPair.fromPrivate("75205699e5fab1e0fa5c3d41898d00f8d251dc38f09ab85b9f5f035f72725310".hexStringToByteArray())
        val fromBigInteger = KeyPair.fromPrivate(BigInteger("75205699e5fab1e0fa5c3d41898d00f8d251dc38f09ab85b9f5f035f72725310", 16))
        assertEquals(fromByteArray, fromBigInteger)
        checkKeyPair(fromByteArray)
        checkKeyPair(fromBigInteger)
    }

    @Test
    fun signAndRecover() {
        val privateKey = "0x8678adf78db8d1c8a40028795077b3463ca06a743ca37dfd28a5b4442c27b457"
        val key = KeyPair.fromPrivate(privateKey.hexStringToByteArray())
        val data = "9b8bc77908c0b0ebe93e897e43f594b811f5d7130d86a5708403ddb417dc111b".hexStringToByteArray()

        val expectedSignature = ECDSASignature(
                "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexAsBigInteger(),
                "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexAsBigInteger()).apply { v = 27 }
        val actualSignature = key.sign(data)
        assertEquals("Unexpected signature", expectedSignature, actualSignature)

        val signatureBase64 = Base64.toBase64String(byteArrayOf(actualSignature.v) + actualSignature.r.toByteArray() + actualSignature.s.toByteArray())
        val recoveredKey = KeyPair.signatureToKey(data, signatureBase64)
        assertTrue(recoveredKey.isPubKeyOnly)
        assertArrayEquals(key.address, recoveredKey.address)

        assertFalse(recoveredKey.hasPrivKey())
        assertThrow({ recoveredKey.sign(data) }, throwablePredicate = { it is MissingPrivateKeyException })
        assertThrow({ recoveredKey.privKey }, throwablePredicate = { it is MissingPrivateKeyException })

        recoveredKey.verify(data, expectedSignature)
    }
}