package io.gnosis.safe.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.gnosis.data.models.AddressInfoExtended
import io.gnosis.data.models.transaction.DataDecoded
import io.gnosis.data.models.transaction.ValueDecoded

class ParamSerializer(
    moshi: Moshi
) {
    private val decodedDataAdapter: JsonAdapter<DataDecoded>
    private val decodedValuesAdapter: JsonAdapter<List<ValueDecoded>>
    private val addressInfoIndexAdapter: JsonAdapter<Map<String, AddressInfoExtended>>

    init {
        decodedDataAdapter = moshi.adapter(DataDecoded::class.java)
        decodedValuesAdapter = moshi.adapter(Types.newParameterizedType(List::class.java, ValueDecoded::class.java))
        addressInfoIndexAdapter = moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, AddressInfoExtended::class.java))
    }

    fun serializeDecodedData(decodedData: DataDecoded): String {
        return decodedDataAdapter.toJson(decodedData)
    }

    fun deserializeDecodedData(decodedDataString: String): DataDecoded? {
        return decodedDataAdapter.fromJson(decodedDataString)
    }

    fun serializeDecodedValues(decodedValues: List<ValueDecoded>): String {
        return decodedValuesAdapter.toJson(decodedValues)
    }

    fun deserializeDecodedValues(decodedValuesString: String): List<ValueDecoded>? {
        return decodedValuesAdapter.fromJson(decodedValuesString)
    }

    fun serializeAddressInfoIndex(addressInfoIndex: Map<String, AddressInfoExtended>?): String? {
        return addressInfoIndex?.let { addressInfoIndexAdapter.toJson(it) }
    }

    fun deserializeAddressInfoIndex(addressInfoIndexString: String?): Map<String, AddressInfoExtended>? {
        return addressInfoIndexString?.let { addressInfoIndexAdapter.fromJson(it) }
    }
}
