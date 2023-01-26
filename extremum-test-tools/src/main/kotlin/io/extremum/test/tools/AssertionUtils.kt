package io.extremum.test.tools

import de.cronn.reflection.util.PropertyUtils
import de.cronn.reflection.util.TypedPropertyGetter
import io.extremum.sharedmodels.basic.BasicModel
import io.extremum.sharedmodels.descriptor.Descriptor
import org.assertj.core.api.ObjectAssert
import org.junit.jupiter.api.Assertions.assertEquals

object AssertionUtils {

    fun assertEqualsDescriptors(exp: Descriptor, actual: Descriptor) {
        assertEquals(exp.externalId, actual.externalId)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : V?, V : BasicModel<*>> ObjectAssert<T>.isNotNullExt(): ObjectAssert<V> =
        this.isNotNull as ObjectAssert<V>

    inline fun <reified T : BasicModel<*>> ObjectAssert<T>.hasFieldWithValue(
        nameGetter: TypedPropertyGetter<T, *>,
        value: Any?
    ): ObjectAssert<T> =
        this.hasFieldOrPropertyWithValue(PropertyUtils.getPropertyName(T::class.java, nameGetter), value)

    fun assertEqualsByUuid(exp: BasicModel<*>, actual: BasicModel<*>) {
        assertEquals(actual.uuid.externalId, exp.uuid.externalId)
    }
}