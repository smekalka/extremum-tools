package io.extremum.model.tools.mapper

import io.extremum.test.tools.StringUtils.assertEqual
import io.extremum.test.tools.StringUtils.toDescriptor
import io.extremum.test.tools.ToJsonFormatter.toJson
import io.extremum.model.tools.mapper.GraphQlListUtils.toGraphQlList
import io.extremum.model.tools.mapper.GraphQlListUtils.toList
import io.extremum.model.tools.mapper.model.Account
import io.extremum.model.tools.mapper.model.Change
import io.extremum.model.tools.mapper.model.Compensation
import io.extremum.model.tools.mapper.model.CompensationWithChange
import io.extremum.model.tools.mapper.model.Event
import io.extremum.model.tools.mapper.model.Experience
import io.extremum.model.tools.mapper.model.Timepoint
import io.extremum.model.tools.mapper.MapperUtils.convertValue
import io.extremum.model.tools.mapper.MapperUtils.mapToObject
import io.extremum.model.tools.mapper.MapperUtils.readValue
import io.extremum.model.tools.mapper.MapperUtils.toStringOrMultilingual
import io.extremum.sharedmodels.basic.MultilingualLanguage
import io.extremum.sharedmodels.basic.StringOrMultilingual
import io.extremum.sharedmodels.basic.StringOrObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

class MapperUtilsTest {

    @Test
    fun readValue() {
        val field1Value = "field1 value"
        val field2Value = 20

        val result = """{"field1": "$field1Value", "field2": $field2Value}""".readValue<TestModel>()

        val exp = TestModel(
            field1 = field1Value,
            field2 = field2Value
        )
        assertEquals(result, exp)
    }

    @Test
    fun convertValue() {
        val field1Value = "field1 value"
        val field2Value = 20

        val result = mapOf(
            "field1" to field1Value,
            "field2" to field2Value,
        ).convertValue<TestModel>()

        val exp = TestModel(
            field1 = field1Value,
            field2 = field2Value
        )
        assertEquals(result, exp)
    }

    private data class TestModel(
        val field1: String? = null,
        val field2: Int? = null,
        val field3: String? = null,
    )

    @Test
    fun `model with ZonedDateTime`() {
        val timepoint =
            """{"timestamp":"2022-11-21T10:23:33.901201Z","uuid":"26445fd0-3f09-46cb-954e-f9c1925f2894"}"""
                .readValue<Timepoint>()
        Assertions.assertNotNull(timepoint.timestamp)
    }

    @Test
    fun `StringOrObject conversions`() {
        data class Param1(
            val param1_1: String,
            val param1_2: String,
        )
        data class CustomProperties(
            val param1: Param1,
            val param2: String
        )
        val properties = CustomProperties(
            param1 = Param1(
                param1_1 = "param1_1 value",
                param1_2 = "param1_2 value",
            ),
            param2 = "param2 value",
        )
        val compensation = Compensation().apply {
            function = "function name"
            parameters = StringOrObject(properties)
            uuid = "123".toDescriptor()
        }
        val serialized = """
{   
    "function": "function name",
    "parameters": {
            "param1": {
                "param1_1": "param1_1 value",
                "param1_2": "param1_2 value"
            },
            "param2": "param2 value"
        },
    "uuid": "123"
}"""
        assertEqual(compensation.toJson(), serialized)

        val deserialized = serialized
            .readValue<Compensation>()
        val deserializedProperties = deserialized.parameters.`object`.convertValue<CustomProperties>()
        assertEquals(deserializedProperties, properties)
    }

    @Test
    fun `StringOrObject special model conversions`() {
        val parameters = Change().apply {
            ordinal = 2.0
        }
        val compensation = CompensationWithChange().apply {
            this.parameters = StringOrObject(parameters)
            uuid = "123".toDescriptor()
        }
        val serialized = """
{   
    "parameters": {
            "ordinal": 2.0,
            "data": null,
            "compensation": null,
            "uuid": null
        },
    "uuid": "123"
}"""
        assertEqual(compensation.toJson(), serialized)

        val deserialized = serialized
            .readValue<CompensationWithChange>()
        // альтернатива:
        // val deserializedProperties = deserialized.parameters.`object`.convertValue<Change>()
        val deserializedParameters = deserialized.parameters.mapToObject<Change>()
        assertEquals(deserializedParameters, parameters)
    }

    @Test
    fun `GraphQlList with StringOrObject object conversions`() {
        data class Data(
            var add: List<String>,
            var remove: List<String>,
        )
        val change = Change().apply {
            data = StringOrObject(Data(add = listOf("val6"), remove = listOf("val1",
                "val2")))
            uuid = "123".toDescriptor()
        }
        val account = Account().apply {
            changes = listOf(change).toGraphQlList()
        }
        val serialized = """
{"value":null,"datatype":null,"changes":{"edges":[{"node":
{   
    "ordinal": null,
    "data": {
        "add": [
          "val6"
        ],
        "remove": [
          "val1",
          "val2"
        ]
    },
    "compensation": null,
    "uuid": "123"
}
}]},"uuid":null}
"""
        assertEqual(account.toJson(), serialized)

        val deserialized = serialized
            .readValue<Account>()
        val deserializedParameters = deserialized.changes.toList().first().data.mapToObject<Data>()
        assertEquals(deserializedParameters, change.data.`object`)
    }

    @Test
    fun `StringOrObject text conversions`() {
        val properties = "string value"
        val compensation = Compensation().apply {
            function = "function name"
            parameters = StringOrObject(properties)
            uuid = "123".toDescriptor()
        }
        val serialized = """
{   
    "function": "function name",
    "parameters": "$properties",
    "uuid": "123"
}"""
        assertEqual(compensation.toJson(), serialized)

        val deserialized = serialized
            .readValue<Compensation>()
        val deserializedProperties = deserialized.parameters.string
        assertEquals(deserializedProperties, properties)
    }

    @Test
    fun `StringOrObject object with empty list`() {
        data class WithStringObjectList(
            val parameters: StringOrObject<Any>
        )
        val event = Event().apply { experiences = listOf<Experience>().toGraphQlList() }
        // без edges в списках приходят объекты, если они в StringOrObject<Any>
        val serialized = """
{   
    "parameters": {"url": null, "size":null, "product": null, "experiences": [], "participants": null, "uuid": null}
}"""

        val deserialized = serialized
            .readValue<WithStringObjectList>()

        val deserializedProperties = deserialized.parameters.mapToObject<Event>()!!.experiences.toList()
        assertThat(deserializedProperties).contains(*event.experiences.toList().toTypedArray())
    }

    @Test
    fun `StringOrObject object with list`() {
        data class WithStringObjectList(
            val parameters: StringOrObject<Event>
        )

        val uuid = "123".toDescriptor()
        val event = Event().apply { experiences = listOf(Experience().apply { this.uuid = uuid }).toGraphQlList() }
        val model = WithStringObjectList(
            parameters = StringOrObject(event)
        )
        val serialized = """
{   
    "parameters":{"url":null,"size":null,"product":null,"experiences":{"edges":[{"node":{"mime":null,"uuid":"${uuid.externalId}"}}]},"participants":null,"uuid":null}
}"""
        assertEqual(model.toJson(), """
{   
    "parameters":{"url":null,"size":null,"product":null,"experiences":{"edges":[{"node":{"mime":null,"uuid":"${uuid.externalId}"}}]},"participants":null,"uuid":null}
}""")

        val deserialized = serialized
            .readValue<WithStringObjectList>()

        val deserializedEvent = deserialized.parameters.mapToObject<Event>()!!
        val deserializedProperties = deserializedEvent.experiences.toList()
        assertThat(deserializedProperties).contains(*event.experiences.toList().toTypedArray())
    }

    @Test
    fun `StringOrObject object with list without edges`() {
        data class WithStringObjectList(
            val parameters: StringOrObject<Event>
        )

        val uuid = "123".toDescriptor()
        val event = Event().apply { experiences = listOf(Experience().apply { this.uuid = uuid }).toGraphQlList() }
        // без edges в списках приходят объекты, если они в StringOrObject<Any>
        val serialized = """
{   
    "parameters":{"url":null,"size":null,"product":null,"experiences":[{"mime":null,"uuid":"${uuid.externalId}"}],"participants":null,"uuid":null}
}"""

        val deserialized = serialized
            .readValue<WithStringObjectList>()

        val deserializedEvent = deserialized.parameters.mapToObject<Event>()!!
        val deserializedProperties = deserializedEvent.experiences.toList()
        assertThat(deserializedProperties).contains(*event.experiences.toList().toTypedArray())
    }

    @Test
    fun `StringOrObject list of string`() {
        data class WithStringObjectList(
            val parameters: StringOrObject<List<String>>
        )
        val list = listOf("3", "2", "1")
        val model = WithStringObjectList(
            parameters = StringOrObject(list)
        )
        val serialized = """
{   
    "parameters": ["3", "2", "1"]
}"""
        assertEqual(model.toJson(), serialized)

        val deserialized = serialized
            .readValue<WithStringObjectList>()
        val deserializedProperties = deserialized.parameters.`object`
        assertThat(deserializedProperties).contains(*list.toTypedArray())
    }

    @Test
    fun `StringOrObject list of list of string`() {
        data class WithStringObjectList(
            val parameters: StringOrObject<List<List<String>>>
        )
        val list = listOf(listOf("3", "2"), listOf("2"), listOf("1"))
        val model = WithStringObjectList(
            parameters = StringOrObject(list)
        )
        val serialized = """
{   
    "parameters": [["3", "2"],["2"],["1"]]
}"""
        assertEqual(model.toJson(), serialized)

        val deserialized = serialized
            .readValue<WithStringObjectList>()
        val deserializedProperties = deserialized.parameters.`object`
        assertThat(deserializedProperties).contains(*list.toTypedArray())
    }

    @Test
    fun `toStringOrMultilingual string`() {
        val string = "text value"
        val resultString = StringOrObject<String>(string).toStringOrMultilingual()
        assertEquals(resultString, StringOrMultilingual(string))

        val map = mapOf(
            MultilingualLanguage.en to "en",
            MultilingualLanguage.de to "de",
        )
        val resultMap = StringOrObject(map).toStringOrMultilingual()
        assertEquals(resultMap, StringOrMultilingual(map))

        val mapNotLanguage =
            mapOf(
                1 to "en",
                2 to "de",
            )
        val resultNotLanguage = StringOrObject(mapNotLanguage).toStringOrMultilingual()
        @Suppress("UNCHECKED_CAST")
        assertEquals(resultNotLanguage, StringOrMultilingual(mapNotLanguage as Map<MultilingualLanguage, String>))

        assertThrows<IllegalStateException> {
            StringOrObject(2).toStringOrMultilingual()
        }
    }
}