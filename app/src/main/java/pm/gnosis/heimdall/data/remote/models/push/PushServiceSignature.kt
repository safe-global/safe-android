package pm.gnosis.heimdall.data.remote.models.push

import com.squareup.moshi.Json
import pm.gnosis.svalinn.accounts.base.models.Signature
import pm.gnosis.utils.decimalAsBigInteger

data class PushServiceSignature(
    @Json(name = "v") val v: Int,
    @Json(name = "r") val r: String,
    @Json(name = "s") val s: String
) {
    fun toSignature() = Signature(
        r.decimalAsBigInteger(),
        s.decimalAsBigInteger(),
        v.toByte()
    )

    companion object {
        fun fromSignature(signature: Signature) = PushServiceSignature(
            signature.v.toInt(),
            signature.r.toString(),
            signature.s.toString()
        )
    }
}
