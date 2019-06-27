package pm.gnosis.heimdall.helpers

import org.junit.Before
import org.junit.Test

import org.junit.Assert.*
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexStringToByteArray

class SvalinnCryptoHelperTest {

    private lateinit var helper: SvalinnCryptoHelper

    @Before
    fun setUp() {
        helper = SvalinnCryptoHelper()
    }

    @Test
    fun signAndRecover() {
        val privateKey = "0x8678adf78db8d1c8a40028795077b3463ca06a743ca37dfd28a5b4442c27b457".hexStringToByteArray()
        val data = "9b8bc77908c0b0ebe93e897e43f594b811f5d7130d86a5708403ddb417dc111b".hexStringToByteArray()

        val expectedSignature = Signature(
            "6c65af8fabdf55b026300ccb4cf1c19f27592a81c78aba86abe83409563d9c13".hexAsBigInteger(),
            "256a9a9e87604e89f083983f7449f58a456ac7929265f7114d585538fe226e1f".hexAsBigInteger(), 27
        )
        val signature = helper.sign(privateKey, data)
        assertEquals(expectedSignature, signature)

        assertEquals(helper.recover(data, expectedSignature), "a5056c8efadb5d6a1a6eb0176615692b6e648313".asEthereumAddress()!!)
    }

}