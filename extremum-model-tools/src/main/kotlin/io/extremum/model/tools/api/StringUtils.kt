package io.extremum.model.tools.api

object StringUtils {

    /**
     * Строит из набора пар ключ-значение строку ненулевых параметров
     * key1=value1&key2=value2...
     */
    fun buildApiParams(vararg params: Pair<String, Any?>): String =
        params
            .filter { (key, value) -> value != null }
            .joinToString("&") { (key, value) ->
                "$key=$value"
            }

    /**
     * Заполняет аргументы в строке с помощью [format]
     */
    fun String.fillArgs(vararg args: Any?): String = String.format(this, *args)

    /**
     * Исходный regex взят из /libexec/src/time/format.go:ParseDuration "[-+]?([0-9]*(\\.[0-9]*)?[a-z]+)+"
     */
    private val validExpirationRegex = "[-+]?([0-9]+(\\.[0-9]*)?(ns|us|µs|ms|s|m|h)+)+".toRegex()

    /**
     * Валидация продолжительности.
     * Формат продолжительности: набор целых или дробных чисел с опциональным знаком и единицей измерения.
     * Пример составляющих: "300ms", "-1.5h" or "2h45m".
     * Возможные единицы времени: "ns", "us" (или "µs"), "ms", "s", "m", "h".
     */
    fun String.validateExpiration() {
        if (!this.matches(validExpirationRegex)) {
            throw IllegalStateException("Expiration '$this' does not match the pattern '$validExpirationRegex'")
        }
    }
}