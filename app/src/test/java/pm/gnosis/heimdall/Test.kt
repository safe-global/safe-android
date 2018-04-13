package pm.gnosis.heimdall

import com.squareup.moshi.*
import org.junit.Assert
import org.junit.Test
import pm.gnosis.crypto.KeyPair
import pm.gnosis.crypto.utils.Sha3Utils
import pm.gnosis.heimdall.data.remote.models.push.PushMessage
import pm.gnosis.heimdall.data.remote.models.push.SendTransactionHashParams
import java.math.BigInteger


class Test {
    val payload = """
        {
    "type": "sendTransactionHash",
    "params": {
      "id": "testId",
      "txHash": "test tx hash"
  }
}

        """

    private val moshi = Moshi.Builder().build()

    @Test
    fun test() {
        Assert.assertTrue(true)
        val type = Types.newParameterizedType(PushMessage::class.java, Any::class.java)
        val adapter = moshi.adapter<PushMessage<*>>(type)
        val pushMessage = adapter.fromJson(payload)
        println(pushMessage)

        if (pushMessage.type == "sendTransactionHash") {
            val newType = Types.newParameterizedType(PushMessage::class.java, SendTransactionHashParams::class.java)
            val newAdapter = moshi.adapter<PushMessage<SendTransactionHashParams>>(newType)
            val newPushMessage = newAdapter.fromJson(payload)
            println(newPushMessage)
        }
    }

    @Test
    fun generateChromeExtensionQRCodePayload() {
        val signature = KeyPair.fromPrivate(CHROME_EXTENSION_PRIVATE_KEY).sign(Sha3Utils.keccak("GNO$EXPIRATION_DATE".toByteArray()))
        println("""
            {
                "expirationDate": "$EXPIRATION_DATE",
                "signature": {
                    "v": ${signature.v},
                    "r": "${signature.r}",
                    "s": "${signature.s}"
                    }
                    }
            """)
    }

    companion object {
        val CHROME_EXTENSION_PRIVATE_KEY = BigInteger("e89adee5dfae278e9774d7a49cc91fdcf505c8c2f47a59ce1238db628f5b35cd", 16)
        const val EXPIRATION_DATE = "2018-07-20T12:48:09+00:00"
    }
}
