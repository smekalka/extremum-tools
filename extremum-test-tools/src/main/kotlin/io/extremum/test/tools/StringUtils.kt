package io.extremum.test.tools

import io.extremum.sharedmodels.basic.StringOrMultilingual
import io.extremum.sharedmodels.descriptor.Descriptor
import org.junit.jupiter.api.Assertions.assertEquals

object StringUtils {

    fun wrapWithQuery(value: String): String = """{
            "query": "$value"
        }"""

    fun assertEqual(actual: String, exp: String) {
        assertEquals(actual.trimToCompare(), exp.trimToCompare())
    }

    private fun String.trimToCompare(): String =
        this
            .trim()
            .replace("\n", " ")
            .replace(Regex("( )*\\{( )*"), "{")
            .replace(Regex("( )*}( )*"), "}")
            .replace(Regex("( )*\\(( )*"), "(")
            .replace(Regex("( )*\\)( )*"), ")")
            .replace(Regex("( )*:( )*"), ":")
            .replace(Regex("( )*\"( )*"), "\"")
            .replace(Regex("( )+"), " ")

    fun String.toStringOrMultilingual(): StringOrMultilingual = StringOrMultilingual(this)

    fun String.toDescriptor(): Descriptor = Descriptor(this)
}