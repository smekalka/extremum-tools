package io.extremum.model.tools.mapper

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.extremum.sharedmodels.basic.StringOrObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Arrays

class StringOrObjectDeserializerTest {

    private var mapper = jacksonObjectMapper().apply {
        val module = SimpleModule().apply {
            addDeserializer(StringOrObject::class.java, StringOrObjectDeserializer())
        }
        registerModule(module)
    }

    @Test
    fun nullValue() {
        val result = readValue("{\"value\":null}", ClassWithStringOrObject::class.java)
        assertNull(result.value)
    }

    @Test
    fun map() {
        val result = readValue(
            "{\"value\":{\"age\":25,\"name\":\"inx\"}}",
            ClassWithStringOrObject::class.java
        )
        val map = mapper.convertValue(result.value?.`object`, MutableMap::class.java)
        assertEquals(
            mapOf(
                "age" to 25,
                "name" to "inx"
            ),
            map
        )
    }

    @Test
    fun `object`() {
        val result = readValue(
            "{\"value\":{\"age\":25,\"name\":\"inx\"}}",
            ClassWithStringOrObject::class.java
        )
        val user: User = mapper.convertValue(result.value?.`object`, User::class.java)
        assertEquals(User("inx", 25), user)
    }

    @Test
    fun simpleTypes() {
        assertConversion("{\"value\":\"str\"}", StringOrObject("str"))
        assertConversion("{\"value\":true}", StringOrObject(true))
        assertConversion("{\"value\":2}", StringOrObject(2))
        assertConversion("{\"value\":2.7}", StringOrObject(2.7))
        assertConversion("{\"value\":[\"s1\", \"s2\"]}", StringOrObject(Arrays.asList("s1", "s2")))
    }

    private fun assertConversion(str: String, exp: StringOrObject<Any>) {
        val result = readValue(str, ClassWithStringOrObject::class.java)
        assertEquals(exp, result.value)
    }

    private fun <T> readValue(str: String, valueClass: Class<T>): T {
        return mapper.readValue(str, valueClass)
    }

    private fun convertValue(value: Any, valueClass: Class<*>): Any? {
        return mapper.convertValue(value, valueClass)
    }

    private data class ClassWithStringOrObject<T>(
        val value: StringOrObject<T>?
    )

    private data class User(
        var name: String,
        val age: Int,
    )
}