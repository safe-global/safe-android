package io.gnosis.safe.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.gnosis.data.models.AddressInfo
import io.gnosis.data.models.transaction.DataDecoded
import io.gnosis.data.models.transaction.DetailedExecutionInfo
import io.gnosis.data.models.transaction.TxData
import io.gnosis.data.models.transaction.ValueDecoded

class ParamSerializer(
    moshi: Moshi
) {
    private val dataAdapter: JsonAdapter<TxData>
    private val executionInfoAdapter: JsonAdapter<DetailedExecutionInfo>
    private val decodedDataAdapter: JsonAdapter<DataDecoded>
    private val decodedValuesAdapter: JsonAdapter<List<ValueDecoded>>
    private val addressInfoIndexAdapter: JsonAdapter<Map<String, AddressInfo>>

    init {
        dataAdapter = moshi.adapter(TxData::class.java)
        executionInfoAdapter = moshi.adapter(DetailedExecutionInfo::class.java)
        decodedDataAdapter = moshi.adapter(DataDecoded::class.java)
        decodedValuesAdapter = moshi.adapter(Types.newParameterizedType(List::class.java, ValueDecoded::class.java))
        addressInfoIndexAdapter = moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, AddressInfo::class.java))
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

    fun serializeAddressInfoIndex(addressInfoIndex: Map<String, AddressInfo>?): String? {
        return addressInfoIndex?.let { addressInfoIndexAdapter.toJson(it) }
    }

    fun deserializeAddressInfoIndex(addressInfoIndexString: String?): Map<String, AddressInfo>? {
        return addressInfoIndexString?.let { addressInfoIndexAdapter.fromJson(it) }
    }

    fun serializeData(data: TxData?): String? {
        return data?.let { dataAdapter.toJson(data) }
    }

    fun deserializeData(dataString: String): TxData? {
        return dataAdapter.fromJson(dataString)
    }

    fun serializeExecutionInfo(executionInfo: DetailedExecutionInfo?): String? {
        return executionInfo?.let { executionInfoAdapter.toJson(executionInfo) }
    }

    fun deserializeExecutionInfo(executionInfoString: String): DetailedExecutionInfo? {
        return executionInfoAdapter.fromJson(executionInfoString)
    }
}
