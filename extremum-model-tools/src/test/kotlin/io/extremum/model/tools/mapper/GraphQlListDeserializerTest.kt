package io.extremum.model.tools.mapper

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.extremum.sharedmodels.basic.GraphQlList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GraphQlListDeserializerTest {

    private var mapper = jacksonObjectMapper().apply {
        val module = SimpleModule().apply {
            addDeserializer(GraphQlList::class.java, GraphQlListDeserializer())
        }
        registerModule(module)
    }

    @Test
    fun nullValue() {
        val result = readValue("{\"value\":null}", ClassWithGraphQlList::class.java)
        assertNull(result.value)
    }

    @Test
    fun list() {
        val result: ClassWithGraphQlList = readValue("{\"value\":[\"s1\",\"s2\"]}", ClassWithGraphQlList::class.java)
        assertEquals(result.value, GraphQlList(listOf("s1", "s2")))
    }

    @Test
    fun emptyList() {
        val result: ClassWithGraphQlList = readValue("{\"value\":[]}", ClassWithGraphQlList::class.java)
        assertEquals(result.value, GraphQlList<Any>())
    }

    @Test
    fun graphQlList() {
        val result: ClassWithGraphQlList = readValue(
            "{\"value\":{\"edges\":[{\"node\":\"s1\"},{\"node\":\"s2\"}]}}",
            ClassWithGraphQlList::class.java
        )
        assertEquals(result.value, GraphQlList(listOf("s1", "s2")))
    }

    @Test
    fun emptyGraphQlList() {
        val result: ClassWithGraphQlList = readValue("{\"value\":{\"edges\":[]}}", ClassWithGraphQlList::class.java)
        assertEquals(result.value, GraphQlList<Any>())
    }

    private fun <T> readValue(str: String, valueClass: Class<T>): T = mapper.readValue(str, valueClass)
    
    private data class ClassWithGraphQlList(
        val value: GraphQlList<String>?
    )
}