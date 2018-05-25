package pm.gnosis.heimdall.data.repositories.models

import android.os.Parcel
import android.os.Parcelable
import pm.gnosis.heimdall.data.repositories.TransactionExecutionRepository.Operation
import pm.gnosis.models.Transaction
import pm.gnosis.models.TransactionParcelable


data class SafeTransaction(val wrapped: Transaction, val operation: Operation) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable<TransactionParcelable>(TransactionParcelable::class.java.classLoader).transaction,
        Operation.values()[parcel.readInt()]
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(wrapped.parcelable(), flags)
        parcel.writeInt(operation.ordinal)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SafeTransaction> {
        override fun createFromParcel(parcel: Parcel) = SafeTransaction(parcel)
        override fun newArray(size: Int): Array<SafeTransaction?> = arrayOfNulls(size)
    }
}
