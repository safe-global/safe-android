package pm.gnosis.android.app.wallet.data.model

import android.os.Parcel
import android.os.Parcelable

data class TransactionDetails(val address: String,
                              val value: String? = null,
                              val gas: String? = null,
                              val data: String? = null) : Parcelable {

    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(address)
        parcel.writeString(value)
        parcel.writeString(gas)
        parcel.writeString(data)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransactionDetails> {
        override fun createFromParcel(parcel: Parcel): TransactionDetails {
            return TransactionDetails(parcel)
        }

        override fun newArray(size: Int): Array<TransactionDetails?> {
            return arrayOfNulls(size)
        }
    }
}
