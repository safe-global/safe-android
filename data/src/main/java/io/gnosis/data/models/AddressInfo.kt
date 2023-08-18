package io.gnosis.data.models

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.gnosis.data.adapters.SolidityAddressParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import pm.gnosis.model.Solidity

@JsonClass(generateAdapter = true)
@Parcelize
@TypeParceler<Solidity.Address, SolidityAddressParceler>
data class AddressInfo(
    @Json(name = "value") val value: Solidity.Address,
    @Json(name = "name") val name: String? = null,
    @Json(name = "logoUri") val logoUri: String? = null
) : Parcelable
