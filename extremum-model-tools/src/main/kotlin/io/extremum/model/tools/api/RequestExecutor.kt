package io.extremum.model.tools.api

import io.extremum.model.tools.mapper.MapperUtils.convertValueSafe
import io.extremum.sharedmodels.dto.Pagination
import io.extremum.sharedmodels.dto.Response
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import java.util.logging.Logger

/**
 * Функционал для выполнения запросов к api, которые возвращают [Response].
 * При получении статуса ответа или статуса в поле [Response.code] не [HttpStatus.OK] или [HttpStatus.NOT_FOUND]
 * возбуждается исключение [ExtremumApiException].
 * При заданном [xAppId] добавляется header [X_APP_ID_HEADER].
 * Заполненные [headers] также добавляются к запросам.
 */
class RequestExecutor(
    private val xAppId: String? = null,
    private var headers: Map<String, String> = mapOf(),
) {
    private val logger: Logger = Logger.getLogger(this::class.java.name)

    fun updateHeaders(headers: Map<String, String>) {
        this.headers = headers
    }

    /**
     * Запрос объекта типа [clazz] из ответа [Response.result] по [requestObj].
     * Возвращается null, если [Response.result] пуст или получен статус [HttpStatus.NOT_FOUND].
     * Возбуждается исключение [ExtremumApiException],
     * если формат [Response.result] не совпадает с заданным типом [clazz].
     */
    suspend fun <T : Any> request(clazz: Class<T>, requestObj: WebClient.RequestHeadersSpec<*>): T? =
        requestRaw(requestObj)
            .getResultFromResponse(clazz)

    /**
     * Generic аналог [request].
     */
    suspend inline fun <reified T : Any> request(requestObj: WebClient.RequestHeadersSpec<*>): T? =
        request(T::class.java, requestObj)

    /**
     * Запрос из ответа по [requestObj] объекта аналогично [request] и [Response.pagination].
     */
    suspend fun <T : Any> requestWithPagination(
        clazz: Class<T>,
        requestObj: WebClient.RequestHeadersSpec<*>
    ): Pair<T?, Pagination?> =
        requestRaw(requestObj)
            .let {
                it.getResultFromResponse(clazz) to it.pagination
            }

    /**
     * Generic аналог [requestWithPagination].
     */
    suspend inline fun <reified T : Any> requestWithPagination(requestObj: WebClient.RequestHeadersSpec<*>): Pair<T?, Pagination?> =
        requestWithPagination(T::class.java, requestObj)

    /**
     * Запрос ответа по [requestObj] в исходном виде [Response].
     * Возбуждается исключение [ExtremumApiException],
     * если получен неуспешный статус заброса (не [HttpStatus.OK] или [HttpStatus.NOT_FOUND]).
     */
    suspend fun requestRaw(requestObj: WebClient.RequestHeadersSpec<*>): Response =
        requestObj
            .addHeaders()
            .awaitExchange { response ->
                val statusCode = response.statusCode()
                logger.info("Response code: $statusCode")
                val body = response.awaitBody<Response>()

                if (body.code !in NOT_FAILED_STATUSES_VALUES) {
                    logger.warning("Error status: $statusCode")
                    if (body.alerts.isNotEmpty()) {
                        logger.warning("Alerts from response body: ${body.alerts}")
                        throw ExtremumApiException(
                            code = statusCode,
                            message = body.alerts.joinToString { it.code + ": " + it.message }
                        )
                    }
                    throw ExtremumApiException(code = body.code, message = "request failed.")
                }

                if (statusCode !in NOT_FAILED_STATUSES) {
                    throw ExtremumApiException(code = statusCode, message = "request failed.")
                }

                body
            }


    private fun <T : WebClient.RequestHeadersSpec<T>> WebClient.RequestHeadersSpec<T>.addHeaders(): WebClient.RequestHeadersSpec<T> =
        apply {
            xAppId?.let {
                this.header(X_APP_ID_HEADER, it)
            }
            headers.forEach { (name, value) ->
                this.header(name, value)
            }
        }

    private fun <T : Any> Response.getResultFromResponse(clazz: Class<T>): T? {
        return this.result?.let { result ->
            result.convertValueSafe(clazz)
                ?: throw ExtremumApiException("Incorrect result type in response: $result. Expected ${clazz.simpleName}")
        }
    }

    companion object {
        const val X_APP_ID_HEADER = "x-app-id"

        val NOT_FAILED_STATUSES: List<HttpStatus> = listOf(
            HttpStatus.OK,
            HttpStatus.NOT_FOUND,
        )
        val NOT_FAILED_STATUSES_VALUES: List<Int> = NOT_FAILED_STATUSES.map { it.value() }
    }
}