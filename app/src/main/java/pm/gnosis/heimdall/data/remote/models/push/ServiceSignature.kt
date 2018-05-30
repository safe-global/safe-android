package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import pm.gnosis.heimdall.data.adapters.DecimalNumber
import pm.gnosis.svalinn.accounts.base.models.Signature
import java.math.BigInteger

@JsonClass(generateAdapter = true)
data class ServiceSignature(
    @Json(name = "r") @field:DecimalNumber val r: BigInteger,
    @Json(name = "s") @field:DecimalNumber val s: BigInteger,
    @Json(name = "v") val v: Int
) {
    fun toSignature() = Signature(r, s, v.toByte())

    companion object {
        fun fromSignature(signature: Signature) = ServiceSignature(
            signature.r,
            signature.s,
            signature.v.toInt()
        )
    }
}
