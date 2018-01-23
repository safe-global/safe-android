package pm.gnosis.crypto

import org.junit.Assert.*
import org.junit.Test
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexStringToByteArray

class ECDSASignatureTest {
    @Test
    fun fromComponent() {
        val expectedSignature = ECDSASignature(
                "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexAsBigInteger(),
                "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexAsBigInteger()).apply { v = 27 }

        assertEquals(expectedSignature,
                ECDSASignature.fromComponents(
                        "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexStringToByteArray(),
                        "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexStringToByteArray(), 27))
    }
}
