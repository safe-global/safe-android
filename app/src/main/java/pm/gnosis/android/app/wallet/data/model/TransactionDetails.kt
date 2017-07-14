package pm.gnosis.android.app.wallet.data.model

import android.os.Parcel
import android.os.Parcelable
import pm.gnosis.android.app.wallet.util.nullOnThrow
import java.math.BigInteger

data class TransactionDetails(val address: BigInteger,
                              val value: Wei? = null,
                              val gas: BigInteger? = null,
                              val data: String? = null) : Parcelable {
    constructor(parcel: Parcel) : this(
            BigInteger(parcel.readString(), 16),
            nullOnThrow { Wei(BigInteger(parcel.readString(), 10)) },
            nullOnThrow { BigInteger(parcel.readString(), 10) },
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(address.toString(16))
        parcel.writeString(value?.value?.toString(10))
        parcel.writeString(gas?.toString(10))
        parcel.writeString(data)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<TransactionDetails> {
        override fun createFromParcel(parcel: Parcel) = TransactionDetails(parcel)
        override fun newArray(size: Int): Array<TransactionDetails?> = arrayOfNulls(size)
    }
}
