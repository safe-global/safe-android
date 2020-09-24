package io.gnosis.safe.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.gnosis.data.backend.dto.DataDecodedDto
import io.gnosis.data.backend.dto.ValueDecodedDto

class ParamSerializer(
    moshi: Moshi
) {
    private val decodedDataAdapter: JsonAdapter<DataDecodedDto>
    private val decodedValuesAdapter: JsonAdapter<List<ValueDecodedDto>>

    init {
        decodedDataAdapter = moshi.adapter(DataDecodedDto::class.java)
        decodedValuesAdapter = moshi.adapter(Types.newParameterizedType(List::class.java, ValueDecodedDto::class.java))
    }

    fun serializeDecodedData(decodedData: DataDecodedDto): String {
        return decodedDataAdapter.toJson(decodedData)
    }

    fun unserializeDecodedData(decodedDataString: String): DataDecodedDto? {
        return decodedDataAdapter.fromJson(decodedDataString)
    }

    fun serializeDecodedValues(decodedValues: List<ValueDecodedDto>): String {
        return decodedValuesAdapter.toJson(decodedValues)
    }

    fun unserializeDecodedValues(decodedValuesString: String): List<ValueDecodedDto>? {
        return decodedValuesAdapter.fromJson(decodedValuesString)
    }
}
