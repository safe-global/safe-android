package pm.gnosis.heimdall.accounts.base.models

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class SignatureTest {
    @Test
    fun testToString() {
        val r = BigInteger("6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13", 16)
        val s = BigInteger("256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f", 16)
        val v = 27.toByte()
        val sig = Signature(r, s, v)
        val encoded = sig.toString()
        assertEquals(
            "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f1b",
            encoded
        )
        assertEquals(sig, Signature.from(encoded))
    }

}