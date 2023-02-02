package io.extremum.model.tools.mapper

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.extremum.model.tools.mapper.MapperUtils.convertToMap
import io.extremum.model.tools.mapper.MapperUtils.convertValue
import io.extremum.model.tools.mapper.MapperUtils.convertValueSafe
import io.extremum.model.tools.mapper.MapperUtils.readValue
import io.extremum.model.tools.mapper.model.Account
import io.extremum.test.tools.ToJsonFormatter.toJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertNull

class MapperUtilsConvertValueTest {

    @Test
    fun `linkedHashMap to map`() {
        val linkedHashMap = linkedMapOf("a" to 1, "b" to 2)
        val result = linkedHashMap.convertValue(Map::class.java)
        assertEquals(linkedHashMap, result)

        val genericResult = linkedHashMap.convertValue<Map<String, Int>>()
        assertEquals(linkedHashMap, genericResult)
    }

    @Test
    fun `map to map with other type`() {
        val jsonNode = "{\"add\":[\"val6\"], \"remove\":[\"val1\",\"val2\"]}".readValue<JsonNode>()
        val mapWithArrayNodes = jsonNode.convertToMap()["_children"]!!
        val result = mapWithArrayNodes.convertValue<Map<String, List<String>>>()
        val add = result["add"] as List<String>
        assertEquals(listOf("val6"), add)
    }

    @Test
    fun `subType to type`() {
        open class ParentClass(
            val a: Int
        )
        class ChildClass(b: Int): ParentClass(b)

        val child = ChildClass(1)
        val result = child.convertValue(ParentClass::class.java)
        assertEquals(child, result)

        val genericResult = child.convertValue<ParentClass>()
        assertEquals(child, genericResult)
    }

    @Test
    fun missingKotlinParameterException() {
        val str = """{
                "field": 1
               }"""
        assertThrows<MissingKotlinParameterException> {
            str.convertValue(Parameters::class.java)
        }
        assertNull(str.convertValueSafe(Parameters::class.java))
        assertNull(str.convertValueSafe<Parameters>())
    }

    @Test
    fun jsonParseException() {
        val str = "any"
        assertThrows<JsonParseException> {
            str.convertValue(Account::class.java)
        }
        assertNull(str.convertValueSafe(Parameters::class.java))
        assertNull(str.convertValueSafe<Parameters>())
    }

    @Test
    fun invalidFormatException() {
        val str = """{
                "i": "ii"
               }"""
        assertThrows<InvalidFormatException> {
            str.convertValue(NestedParameters::class.java)
        }
        assertNull(str.convertValueSafe(Parameters::class.java))
        assertNull(str.convertValueSafe<Parameters>())
    }

    @ParameterizedTest
    @MethodSource("toParameters")
    fun convertValue(case: TestCase) {
        with(case) {
            val result = parameters.convertValue(Parameters::class.java)
            assertEquals(exp, result)
        }
    }

    @ParameterizedTest
    @MethodSource("toParameters")
    fun `generic convertValue`(case: TestCase) {
        with(case) {
            val result = parameters.convertValue<Parameters>()
            assertEquals(exp, result)
        }
    }

    private companion object {
        @Suppress("unused")
        @JvmStatic
        fun toParameters() = arrayOf(
            arrayOf(TestCase("map", initParameters.convertToMap())),
            arrayOf(TestCase("string", initParameters.toJson())),
            arrayOf(TestCase("same type", initParameters)),
        )

        val initParameters = Parameters(
            iN = null,
            i = 1,
            s = "2",
            l = listOf("3", "4"),
            nested = NestedParameters(5)
        )
    }

    data class TestCase(
        val desc: String,
        val parameters: Any,
        val exp: Any = initParameters
    )

    data class Parameters(
        val iN: Int?,
        val i: Int,
        val s: String,
        val l: List<String>,
        val nested: NestedParameters
    )

    data class NestedParameters(
        val i: Int
    )
}