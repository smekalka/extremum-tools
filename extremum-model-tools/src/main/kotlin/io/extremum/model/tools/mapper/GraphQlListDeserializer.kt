package io.extremum.model.tools.mapper

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import io.extremum.model.tools.mapper.GraphQlListUtils.LIST_ELEMENT_FIELD_NAME
import io.extremum.model.tools.mapper.GraphQlListUtils.LIST_FIELD_NAME
import io.extremum.model.tools.mapper.MapperUtils.convertValue
import io.extremum.model.tools.mapper.GraphQlListUtils.toGraphQlList
import io.extremum.sharedmodels.basic.GraphQlList

class GraphQlListDeserializer : JsonDeserializer<GraphQlList<*>>(), ContextualDeserializer {

    private var valueType: JavaType? = null

    override fun createContextual(ctxt: DeserializationContext?, property: BeanProperty): JsonDeserializer<*> {
        val propertyType = property.type
        val valueType = propertyType.containedType(0)
        return GraphQlListDeserializer().apply { this.valueType = valueType }
    }

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): GraphQlList<*> {
        val node = parser.codec.readTree<JsonNode>(parser)
        val valueClass = valueType!!.rawClass

        return when {
            node.isArray -> node.iterator().asSequence().toList()
                .map {
                    it.convertValue(valueClass)
                }
                .toGraphQlList()

            node.isObject -> node.get(LIST_FIELD_NAME).map {
                it.get(LIST_ELEMENT_FIELD_NAME).convertValue(valueClass)
            }
                .toGraphQlList()

            else -> GraphQlList<JsonNode>()
        }
    }
}