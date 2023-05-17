package io.extremum.model.tools.api

import io.extremum.model.tools.api.StringUtils.fillArgs
import io.extremum.model.tools.api.StringUtils.validateExpiration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class StringUtilsTest {

    @Test
    fun buildApiParams() {
        assertThat(StringUtils.buildApiParams()).isEqualTo("")
        assertThat(
            StringUtils.buildApiParams(
                "id" to 1,
                "name" to null,
                "nick" to "Jack"
            )
        ).isEqualTo("id=1&nick=Jack")
    }

    @Test
    fun fillArgs() {
        assertThat("name is %s".fillArgs("Jack")).isEqualTo("name is Jack")
    }

    @ParameterizedTest
    @MethodSource("validateExpirationCases")
    fun validateExpiration(case: TestCase) {
        with(case) {
            if (!exp) {
                assertThrows<IllegalStateException> {
                    value.validateExpiration()
                }
            } else {
                value.validateExpiration()
            }
        }
    }

    private companion object {
        @Suppress("unused")
        @JvmStatic
        fun validateExpirationCases() = arrayOf(
            arrayOf(TestCase(value = "300ms", exp = true)),
            arrayOf(TestCase(value = "300ms", exp = true)),
            arrayOf(TestCase(value = "+300ms", exp = true)),
            arrayOf(TestCase(value = "-1.5h", exp = true)),
            arrayOf(TestCase(value = "2h45m", exp = true)),
            arrayOf(TestCase(value = "2t45m", exp = false)),
            arrayOf(TestCase(value = "s45m", exp = false)),
            arrayOf(TestCase(value = "--1.5h", exp = false)),
        )
    }

    data class TestCase(
        val value: String,
        val exp: Boolean
    )
}