package io.extremum.test.tools

import io.extremum.model.tools.mapper.MapperUtils.convertToMap
import io.extremum.model.tools.mapper.MapperUtils.hasData
import io.extremum.sharedmodels.basic.StringOrMultilingual
import io.extremum.sharedmodels.basic.StringOrObject
import io.extremum.sharedmodels.descriptor.Descriptor

/**
 * Для составления json в тестах.
 * Урезанная копия io.extremum.graphQlClient.builder.util.ValueGraphQlFormatter (io.extremum:graphql-client).
 * Различие с ValueGraphQlFormatter: key обрамляются в ".
 * Пример: {"url": "event url"}
 */
object ToJsonFormatter {

    fun Any?.toJson(): String = this.format(false)

    private fun Any?.format(filterEmpty: Boolean = true): String =
        when {
            this == null -> "null"
            this is Collection<*> -> processCollection(this, filterEmpty)
            this is Double -> this.toString()
            this is String -> getAsQuotedString(this.toString())
            this is Descriptor -> getAsQuotedString(this.externalId)
            this is StringOrMultilingual -> processStringOrMultilingual(this, filterEmpty)
            this is StringOrObject<*> -> processStringOrObject(this, filterEmpty)
            this is Int -> this.toString()

            this is Map<*, *> -> processMap(this, filterEmpty)
            else -> processObject(this, filterEmpty)
        }

    private fun processStringOrMultilingual(stringOrMultilingual: StringOrMultilingual, filterEmpty: Boolean): String =
        with(stringOrMultilingual) {
            if (isTextOnly) text.format(filterEmpty) else multilingualContent.format(filterEmpty)
        }

    private fun processStringOrObject(stringOrObject: StringOrObject<*>, filterEmpty: Boolean): String =
        with(stringOrObject) {
            if (isSimple) string.format(filterEmpty) else `object`.format(filterEmpty)
        }

    private fun processCollection(collection: Collection<*>, filterEmpty: Boolean): String =
        "[" + collection.joinToString(",") { it.format(filterEmpty) } + "]"

    private fun processMap(map: Map<*, *>, filterEmpty: Boolean): String {
        val properties = map
            .let {
                if (filterEmpty) {
                    it.filterValues { value ->
                        value.hasData()
                    }
                } else {
                    it
                }
            }
            .map { (key, value) -> propertyWithValue(key as String, value, filterEmpty) }
        return "{" + properties.joinToString(", ") + "}"
    }

    private fun propertyWithValue(key: String, value: Any?, filterEmpty: Boolean): String =
        "\"" + key + "\": " + value.format(filterEmpty)

    private fun getAsQuotedString(value: String): String {
        val builder = java.lang.StringBuilder()
        builder.append('"')
        for (c in value.toCharArray()) {
            when (c) {
                '"', '\\' -> {
                    builder.append('\\')
                    builder.append(c)
                }

                '\r' -> builder.append("\\r")
                '\n' -> builder.append("\\n")
                else -> if (c.toInt() < 0x20) {
                    builder.append(String.format("\\u%04x", c.toInt()))
                } else {
                    builder.append(c)
                }
            }
        }
        builder.append('"')
        return builder.toString()
    }

    private fun processObject(value: Any, filterEmpty: Boolean): String =
        processMap(value.convertToMap(), filterEmpty)
}