package io.gnosis.data.adapters

import com.squareup.moshi.*
import io.gnosis.data.models.transaction.Param
import io.gnosis.data.models.transaction.ValueDecoded
import pm.gnosis.utils.asEthereumAddress
import pm.gnosis.utils.asEthereumAddressString


class ParamAdapter {

    @ToJson
    fun toJson(writer: JsonWriter, param: Param, valueDecodedAdapter: JsonAdapter<List<ValueDecoded>>) {

        writer.beginObject()

        writer.name("name")
        writer.value(param.name)

        writer.name("type")
        writer.value(param.type)

        writer.name("value")
        when (param) {
            is Param.Address -> {
                writer.value(param.value.asEthereumAddressString())
            }
            is Param.Array -> {
                writer.beginArray()
                param.value.forEach {
                    if(it is List<*>) {
                        writeArray(writer, it as List<Any>)
                    } else {
                        writer.value(it as String)
                    }
                }
                writer.endArray()
            }
            is Param.Bytes -> {
                writer.value(param.value)
                if (param.valueDecoded != null) {
                    writer.name("valueDecoded")
                    valueDecodedAdapter.toJson(writer, param.valueDecoded)
                }
            }
            is Param.Value -> {
                writer.value(param.value as String)
            }
            else -> {}
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
    fun fromJson(reader: JsonReader, valueDecodedAdapter: JsonAdapter<List<ValueDecoded>>): Param {
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
                                    return Param.Address(type, name, value)
                                }
                                type == "bytes" -> {
                                    val value = reader.nextString()
                                    if(reader.peek() ==  JsonReader.Token.NAME && reader.nextName() == "valueDecoded") {
                                        if(reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                                            kotlin.runCatching {
                                                valueDecodedAdapter.fromJson(reader)
                                            }.onSuccess {
                                                reader.endObject()
                                                return Param.Bytes(type, name, value, it)
                                            }.onFailure {
                                                reader.endArray()
                                                reader.endObject()
                                                return Param.Bytes(type, name, value, null)
                                            }
                                        } else {
                                            reader.skipValue()
                                            reader.endObject()
                                            return Param.Bytes(type, name, value, null)
                                        }
                                    } else {
                                        reader.endObject()
                                        return Param.Bytes(type, name, value, null)
                                    }
                                }
                                !type.contains("[") && !type.contains("(") -> {
                                    val value = reader.nextString()
                                    reader.endObject()
                                    return Param.Value(type, name, value)
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
                                        return Param.Array(type, name, value)
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
            return Param.Unknown
        }
        return Param.Unknown
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
