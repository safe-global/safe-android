package pm.gnosis.heimdall.utils

import android.os.Parcel
import kotlinx.android.parcel.Parceler
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexAsBigInteger
import pm.gnosis.utils.hexAsBigIntegerOrNull
import pm.gnosis.utils.toHexString

object SolidityAddressParceler : Parceler<Solidity.Address> {
    override fun create(parcel: Parcel) = Solidity.Address(parcel.readString().hexAsBigInteger())

    override fun Solidity.Address.write(parcel: Parcel, flags: Int) {
        parcel.writeString(value.toHexString())
    }
}

object OptionalSolidityAddressParceler : Parceler<Solidity.Address?> {
    override fun create(parcel: Parcel) = parcel.readString().hexAsBigIntegerOrNull()?.let { Solidity.Address(it) }

    override fun Solidity.Address?.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this?.value?.toHexString() ?: "")
    }
}
