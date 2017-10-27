package pm.gnosis.models

import android.os.Parcel
import android.os.Parcelable
import pm.gnosis.utils.*

data class TransactionParcelable(val transaction: Transaction) : Parcelable {
    constructor(parcel: Parcel) : this(Transaction(
            parcel.readString().hexAsBigInteger(),
            nullOnThrow { Wei(parcel.readString().hexAsBigInteger()) },
            nullOnThrow { parcel.readString().decimalAsBigInteger() },
            nullOnThrow { parcel.readString().decimalAsBigInteger() },
            parcel.readString(),
            nullOnThrow { parcel.readString().decimalAsBigInteger() }))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(transaction.address.asEthereumAddressString())
        parcel.writeString(transaction.value?.value?.asDecimalString())
        parcel.writeString(transaction.gas?.asDecimalString())
        parcel.writeString(transaction.gasPrice?.asDecimalString())
        parcel.writeString(transaction.data)
        parcel.writeString(transaction.nonce?.asDecimalString())
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<TransactionParcelable> {
        override fun createFromParcel(parcel: Parcel) = TransactionParcelable(parcel)
        override fun newArray(size: Int): Array<TransactionParcelable?> = arrayOfNulls(size)
    }
}
