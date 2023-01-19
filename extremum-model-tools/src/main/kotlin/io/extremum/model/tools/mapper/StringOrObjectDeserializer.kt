package io.extremum.model.tools.mapper

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.extremum.sharedmodels.basic.StringOrObject

class StringOrObjectDeserializer @JvmOverloads constructor(vc: Class<*>? = null) :
    StdDeserializer<StringOrObject<*>>(vc) {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): StringOrObject<*> {
        val node = jp.codec.readTree<JsonNode>(jp)
        return getStringOrObject(node)
    }

    private fun getStringOrObject(node: JsonNode): StringOrObject<*> =
        if (node.isValueNode) {
            when {
                node.isTextual -> StringOrObject<String>(node.asText())
                node.isBoolean -> StringOrObject(node.asBoolean())
                node.isInt -> StringOrObject(node.asInt())
                node.isLong -> StringOrObject(node.asLong())
                node.isBigInteger -> StringOrObject(node.bigIntegerValue())
                node.isDouble -> StringOrObject(node.doubleValue())
                node.isFloat -> StringOrObject(node.floatValue())
                node.isBigDecimal -> StringOrObject(node.decimalValue())
                node.isNull -> StringOrObject<String>("")
                else -> StringOrObject<String>("")
            }
        } else {
            when {
                node.isArray -> StringOrObject(node.iterator().asSequence().toList().map { getNodeValue(it) })
                node.isObject -> {
                    val map = node.fieldNames().asSequence().map { it to node.get(it) }.toList().toMap()
                    StringOrObject(map)
                }

                node.isMissingNode -> StringOrObject(mapOf<String, Any?>())
                else -> StringOrObject(mapOf<String, Any?>())
            }
        }

    private fun getNodeValue(node: JsonNode): Any =
        if (node.isValueNode) {
            when {
                node.isTextual -> node.asText()
                node.isBoolean -> node.asBoolean()
                node.isInt -> node.asInt()
                node.isLong -> node.asLong()
                node.isBigInteger -> node.bigIntegerValue()
                node.isDouble -> node.doubleValue()
                node.isFloat -> node.floatValue()
                node.isBigDecimal -> node.decimalValue()
                node.isNull -> ""
                else -> ""
            }
        } else {
            when {
                node.isArray -> node.iterator().asSequence().toList().map { getNodeValue(it) }
                node.isObject -> node.fieldNames().asSequence().map { it to node.get(it) }.toList().toMap()
                node.isMissingNode -> mapOf<String, Any?>()
                else -> mapOf<String, Any?>()
            }
        }
}