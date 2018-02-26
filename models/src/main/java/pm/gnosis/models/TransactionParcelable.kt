package pm.gnosis.models

import android.os.Parcel
import android.os.Parcelable
import pm.gnosis.utils.asEthereumAddressString
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.nullOnThrow

data class TransactionParcelable(val transaction: Transaction) : Parcelable {
    constructor(parcel: Parcel) : this(
        Transaction(
            parcel.readString().hexAsBigInteger(),
            nullOnThrow { Wei(parcel.readString().hexAsBigInteger()) },
            nullOnThrow { parcel.readString().hexAsBigInteger() },
            nullOnThrow { parcel.readString().hexAsBigInteger() },
            parcel.readString(),
            nullOnThrow { parcel.readString().hexAsBigInteger() })
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(transaction.address.asEthereumAddressString())
        parcel.writeString(transaction.value?.value?.toString(16))
        parcel.writeString(transaction.gas?.toString(16))
        parcel.writeString(transaction.gasPrice?.toString(16))
        parcel.writeString(transaction.data)
        parcel.writeString(transaction.nonce?.toString(16))
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<TransactionParcelable> {
        override fun createFromParcel(parcel: Parcel) = TransactionParcelable(parcel)
        override fun newArray(size: Int): Array<TransactionParcelable?> = arrayOfNulls(size)
    }
}
