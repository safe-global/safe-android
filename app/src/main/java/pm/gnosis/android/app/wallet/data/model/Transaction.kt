package pm.gnosis.android.app.wallet.data.model

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json
import pm.gnosis.android.app.wallet.util.hexToBigInteger
import timber.log.Timber
import java.math.BigInteger

data class Transaction(
        val nonce: String,
        val to: String,
        val data: String,
        val value: BigInteger,
        val gasLimit: BigInteger,
        val gasPrice: BigInteger,
        val v: BigInteger,
        val r: BigInteger,
        val s: BigInteger)

data class TransactionJson(
        @Json(name = "nonce") val nonce: String,
        @Json(name = "to") val to: String,
        @Json(name = "data") val data: String,
        @Json(name = "value") val value: String,
        @Json(name = "gasLimit") val gasLimit: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "v") val v: String,
        @Json(name = "r") val r: String,
        @Json(name = "s") val s: String) : Parcelable {

    fun read(): Transaction? {
        return try {
            Transaction(
                    nonce = nonce,
                    to = to,
                    data = data,
                    value = value.hexToBigInteger(),
                    gasLimit = gasLimit.hexToBigInteger(),
                    gasPrice = gasPrice.hexToBigInteger(),
                    v = v.hexToBigInteger(),
                    r = r.hexToBigInteger(),
                    s = s.hexToBigInteger()
            )
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(nonce)
        parcel.writeString(to)
        parcel.writeString(data)
        parcel.writeString(value)
        parcel.writeString(gasLimit)
        parcel.writeString(gasPrice)
        parcel.writeString(v)
        parcel.writeString(r)
        parcel.writeString(s)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransactionJson> {
        override fun createFromParcel(parcel: Parcel): TransactionJson {
            return TransactionJson(parcel)
        }

        override fun newArray(size: Int): Array<TransactionJson?> {
            return arrayOfNulls(size)
        }
    }
}