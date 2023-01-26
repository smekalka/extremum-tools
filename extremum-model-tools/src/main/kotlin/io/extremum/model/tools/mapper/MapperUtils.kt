package io.extremum.model.tools.mapper

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.extremum.sharedmodels.basic.GraphQlList
import io.extremum.sharedmodels.basic.MultilingualLanguage
import io.extremum.sharedmodels.basic.StringOrMultilingual
import io.extremum.sharedmodels.basic.StringOrObject
import org.apache.commons.lang3.SerializationUtils
import java.io.Serializable
import kotlin.reflect.full.isSubclassOf

object MapperUtils {
    val mapper = jacksonObjectMapper().apply {
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        serializerFactory = CustomSerializerFactory.instance
        registerModule(JavaTimeModule())

        val module = SimpleModule().apply {
            addDeserializer(StringOrObject::class.java, StringOrObjectDeserializer())
            addDeserializer(GraphQlList::class.java, GraphQlListDeserializer())
        }
        registerModule(module)
    }

    inline fun <reified R> Any?.convertValue(): R = mapper.convertValue(this, R::class.java)

    fun Any?.convertValue(to: Class<*>): Any = mapper.convertValue(this, to)

    inline fun <reified T> String.readValue(): T = mapper.readValue(this, T::class.java)

    /**
     * Конвертация в одноуровневую map.
     * Добавляются поля, указанные в [filterFields].
     * Если список пуст в [filterFields], то добавляются все.
     */
    fun Any.convertToMap(filterFields: List<String> = listOf()): Map<String, Any?> {
        val clazz = this::class.java
        if (this is Map<*, *>) {
            return this.map { (key, value) -> key.toString() to value }.toMap()
        }
        return convertToMap(clazz, this).let {
            if (filterFields.isEmpty()) {
                it
            } else {
                it.filterKeys { key -> filterFields.contains(key) }
            }
        }
    }

    private fun convertToMap(clazz: Class<out Any>, value: Any): Map<String, Any?> {
        val fields = clazz.declaredFields
        val fieldsWithValues = fields.mapNotNull { field ->
            val fieldName = field.name
            field.isAccessible = true
            val prevValue = field.get(value)
            fieldName to prevValue
        }.toMap()

        val superclass = clazz.superclass
        return if (superclass == null) fieldsWithValues else fieldsWithValues + convertToMap(superclass, value)
    }

    /**
     * Имеет ли объект информацию.
     * Не имеет в одном из следующих случаев:
     *   - null
     *   - пустая коллекция
     *   - boolean: false
     */
    fun Any?.hasData(): Boolean =
        this != null &&
                (!this::class.isSubclassOf(Collection::class) || (this as Collection<*>).isNotEmpty()) &&
                (this::class != GraphQlList::class || (this as GraphQlList<*>).edges?.isNotEmpty() == true) &&
                (this::class != Boolean::class || (this as Boolean))

    fun <T : Serializable> T.copy(blockToApply: T.() -> Unit): T = SerializationUtils.clone(this).apply(blockToApply)

    /**
     * StringOrObject с объектом десериализуется в StringOrObject с map.
     * С помощью метода такой StringOrObject можно сконвертировать в нужный объект.
     */
    inline fun <reified T> StringOrObject<*>.mapToObject(): T? {
        if (`object` is Map<*, *>) return `object`.convertValue()

        if (`object` == null) return null

        try {
            return `object` as T
        } catch (e: Exception) {
            throw IllegalStateException("Can't convert StringOrObject.object of $this to type ${T::class.java.simpleName}")
        }
    }

    fun StringOrObject<*>.toStringOrMultilingual(): StringOrMultilingual {
        if (string != null) return StringOrMultilingual(string)

        try {
            if (`object` is Map<*, *>) {
                return StringOrMultilingual(`object` as Map<MultilingualLanguage, String>)
            }
        } catch (e: Exception) {
            // throw next
        }
        throw IllegalStateException(
            "Can't convert StringOrObject $this to StringOrMultilingual. " +
                    "String or Map<MultilingualLanguage, String> are allowed in StringOrObject for conversion."
        )
    }
}