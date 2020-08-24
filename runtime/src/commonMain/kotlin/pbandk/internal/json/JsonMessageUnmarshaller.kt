package pbandk.internal.json

import kotlinx.serialization.json.*
import kotlinx.serialization.parse
import pbandk.*
import pbandk.internal.underscoreToCamelCase
import pbandk.json.JsonConfig

private val FieldDescriptor<*, *>.jsonNames: List<String>
    get() = listOf(
        jsonName ?: name.underscoreToCamelCase(),
        name
    )

internal class JsonMessageUnmarshaller internal constructor(
    private val content: JsonElement,
    private val jsonConfig: JsonConfig
) : MessageUnmarshaller {
    private val jsonValueUnmarshaller = JsonValueUnmarshaller(jsonConfig)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Message> readMessage(
        messageCompanion: Message.Companion<T>,
        fieldFn: (Int, Any) -> Unit
    ): Map<Int, UnknownField> = try {
        if (content is JsonNull) throw InvalidProtocolBufferException("top-level message must not be null")
        readMessageObject(messageCompanion, content, fieldFn)
        emptyMap()
    } catch (e: InvalidProtocolBufferException) {
        throw e
    } catch (e: Exception) {
        throw InvalidProtocolBufferException("unable to read message", e)
    }

    private fun <T : Message> readMessageObject(
        messageCompanion: Message.Companion<T>,
        content: JsonElement,
        fieldFn: (Int, Any) -> Unit
    ) {
        for ((key, jsonValue) in content.jsonObject) {
            val fd = messageCompanion.descriptor.fields.firstOrNull { key in it.jsonNames }
                ?: if (jsonConfig.ignoreUnknownFieldsInInput) {
                    continue
                } else {
                    throw InvalidProtocolBufferException("Unknown field name and ignoreUnknownFieldsInInput=false: $key")
                }
            if (jsonValue is JsonNull) continue
            fieldFn(fd.number, jsonValueUnmarshaller.readValue(jsonValue, fd.type))
        }
    }

    companion object {
        fun fromString(data: String, jsonConfig: JsonConfig = JsonConfig.DEFAULT): JsonMessageUnmarshaller {
            val content = Json.decodeFromString(JsonElement.serializer(), data)
            return JsonMessageUnmarshaller(content, jsonConfig)
        }
    }
}
