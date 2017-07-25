package pm.gnosis.android.app.wallet.data.model

import android.os.Parcel
import android.os.Parcelable
import pm.gnosis.android.app.wallet.util.*
import java.math.BigInteger

data class TransactionDetails(val address: BigInteger,
                              val value: Wei? = null,
                              val gas: BigInteger? = null,
                              val gasPrice: BigInteger? = null,
                              val data: String? = null,
                              val nonce: BigInteger? = null) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString().hexAsBigInteger(),
            nullOnThrow { Wei(parcel.readString().hexAsBigInteger()) },
            nullOnThrow { parcel.readString().decimalAsBigInteger() },
            nullOnThrow { parcel.readString().decimalAsBigInteger() },
            parcel.readString(),
            nullOnThrow { parcel.readString().decimalAsBigInteger() })

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(address.asHexString())
        parcel.writeString(value?.value?.asDecimalString())
        parcel.writeString(gas?.asDecimalString())
        parcel.writeString(gasPrice?.asDecimalString())
        parcel.writeString(data)
        parcel.writeString(nonce?.asDecimalString())
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<TransactionDetails> {
        override fun createFromParcel(parcel: Parcel) = TransactionDetails(parcel)
        override fun newArray(size: Int): Array<TransactionDetails?> = arrayOfNulls(size)
    }
}
