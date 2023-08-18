package io.gnosis.data.adapters

import android.os.Parcel
import kotlinx.parcelize.Parceler
import pm.gnosis.model.Solidity
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString

object SolidityAddressParceler: Parceler<Solidity.Address> {
    override fun create(parcel: Parcel): Solidity.Address {
        return parcel.readString()!!.asEthereumAddress()!!
    }

    override fun Solidity.Address.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this.asEthereumAddressString())
    }
}

object SolidityAddressNullableParceler: Parceler<Solidity.Address?> {
    override fun create(parcel: Parcel): Solidity.Address? {
        return parcel.readString()?.asEthereumAddress()
    }

    override fun Solidity.Address?.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this?.asEthereumAddressString())
    }
}
