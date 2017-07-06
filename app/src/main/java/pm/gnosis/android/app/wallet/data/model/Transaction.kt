package pm.gnosis.android.app.wallet.data.model

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.Json

data class Transaction(
        @Json(name = "nonce") val nonce: String,
        @Json(name = "to") val to: String,
        @Json(name = "data") val data: String,
        @Json(name = "value") val value: String,
        @Json(name = "gasLimit") val gasLimit: String,
        @Json(name = "gasPrice") val gasPrice: String,
        @Json(name = "v") val v: String,
        @Json(name = "r") val r: String,
        @Json(name = "s") val s: String) : Parcelable {

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

    companion object CREATOR : Parcelable.Creator<Transaction> {
        override fun createFromParcel(parcel: Parcel): Transaction {
            return Transaction(parcel)
        }

        override fun newArray(size: Int): Array<Transaction?> {
            return arrayOfNulls(size)
        }
    }
}
