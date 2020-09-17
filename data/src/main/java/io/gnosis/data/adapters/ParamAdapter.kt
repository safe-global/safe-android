package io.gnosis.data.adapters

import com.squareup.moshi.*
import io.gnosis.data.backend.dto.ParamDto
import io.gnosis.data.backend.dto.ValueDecodedDto
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString


class ParamAdapter {

    @ToJson
    fun toJson(writer: JsonWriter, paramDto: ParamDto, valueDecodedAdapter: JsonAdapter<List<ValueDecodedDto>>) {

        writer.beginObject()

        writer.name("name")
        writer.value(paramDto.name)

        writer.name("type")
        writer.value(paramDto.type)

        writer.name("value")
        when (paramDto) {
            is ParamDto.AddressParam -> {
                writer.value(paramDto.value.asEthereumAddressString())
            }
            is ParamDto.ArrayParam -> {
                writer.beginArray()
                paramDto.value.forEach {
                    if(it is List<*>) {
                        writeArray(writer, it as List<Any>)
                    } else {
                        writer.value(it as String)
                    }
                }
                writer.endArray()
            }
            is ParamDto.BytesParam -> {
                writer.value(paramDto.value)
                if (paramDto.valueDecoded != null) {
                    writer.name("valueDecoded")
                    valueDecodedAdapter.toJson(writer, paramDto.valueDecoded)
                }
            }
            is ParamDto.ValueParam -> {
                writer.value(paramDto.value as String)
            }
        }

        writer.endObject()
    }

    private fun writeArray(writer: JsonWriter, value: List<Any>) {
        writer.beginArray()
        value.forEach {
            if(it is List<*>) {
                writeArray(writer, it as List<Any>)
            } else {
                writer.value(it as String)
            }
        }
        writer.endArray()
    }

    @FromJson
    fun fromJson(reader: JsonReader, valueDecodedAdapter: JsonAdapter<List<ValueDecodedDto>>): ParamDto {
        try {
            var name = ""
            var type = getType(reader)
            reader.beginObject()
            while(reader.hasNext()) {
                if (reader.peek() == JsonReader.Token.NAME) {
                    when (reader.nextName()) {
                        "name" -> {
                            name = reader.nextString()
                        }
                        "value" -> {
                            when {
                                type == "address" -> {
                                    val value = reader.nextString().asEthereumAddress()!!
                                    reader.endObject()
                                    return ParamDto.AddressParam(type, name, value)
                                }
                                type == "bytes" -> {
                                    val value = reader.nextString()
                                    if(reader.peek() ==  JsonReader.Token.NAME && reader.nextName() == "valueDecoded") {
                                        if(reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                                            val valueDecoded = valueDecodedAdapter.fromJson(reader)
                                            reader.endObject()
                                            return ParamDto.BytesParam(type, name, value, valueDecoded)
                                        } else {
                                            reader.skipValue()
                                            reader.endObject()
                                            return ParamDto.BytesParam(type, name, value, null)
                                        }
                                    } else {
                                        reader.endObject()
                                        return ParamDto.BytesParam(type, name, value, null)
                                    }
                                }
                                !type.contains("[") -> {
                                    val value = reader.nextString()
                                    reader.endObject()
                                    return ParamDto.ValueParam(type, name, value)
                                }
                                else -> {
                                    val value = mutableListOf<Any>()
                                    if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                                        reader.beginArray()
                                        while(reader.peek() != JsonReader.Token.END_ARRAY) {
                                            reader.readJsonValue()?.let {
                                                value.add(it)
                                            }
                                        }
                                        reader.endArray()
                                        reader.endObject()
                                        return ParamDto.ArrayParam(type, name, value)
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                            }
                        }
                        else -> {
                            reader.skipValue()
                        }
                    }
                }
            }
            reader.endObject()
        } catch (e: Exception) {
            return ParamDto.UnknownParam
        }
        return ParamDto.UnknownParam
    }

    private fun getType(reader: JsonReader): String {
        var type = ""
        val peek = reader.peekJson()
        loop@ while (peek.hasNext()) {
            when(peek.peek()) {
                JsonReader.Token.BEGIN_OBJECT -> {
                    peek.beginObject()
                }
                JsonReader.Token.NAME -> {
                    if(peek.nextName() == "type") {
                        type = peek.nextString()
                        break@loop
                    } else {
                        peek.skipValue()
                    }
                }
                else -> {
                    peek.skipName()
                    peek.skipValue()
                }
            }
        }
        return type
    }
}
