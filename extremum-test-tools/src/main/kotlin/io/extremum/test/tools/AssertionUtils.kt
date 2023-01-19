package io.extremum.test.tools

import de.cronn.reflection.util.PropertyUtils
import de.cronn.reflection.util.TypedPropertyGetter
import io.extremum.sharedmodels.basic.BasicModel
import io.extremum.sharedmodels.descriptor.Descriptor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert

object AssertionUtils {

    fun assertEqualsDescriptors(exp: Descriptor, actual: Descriptor) {
        assertThat(actual.externalId).isEqualTo(exp.externalId)
            .withFailMessage("exp: $exp\nact: $actual")
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
        assertThat(actual.uuid.externalId).isEqualTo(exp.uuid.externalId)
            .withFailMessage("exp: $exp\nact: $actual")
    }
}