package io.extremum.model.tools.api

import io.extremum.model.tools.mapper.MapperUtils.convertValue
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
 */
object RequestExecutor {

    private val logger: Logger = Logger.getLogger(this::class.java.name)

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
     * Запрос списка объектов типа [clazz] из ответа [Response.result] по [requestObj].
     * Возвращается пустой список, если [Response.result] пуст или получен статус [HttpStatus.NOT_FOUND].
     * Возбуждается исключение [ExtremumApiException],
     * если формат [Response.result] не список или один из его объектов не совпадает с заданным типом [clazz].
     */
    suspend fun <T : Any> requestList(clazz: Class<T>, requestObj: WebClient.RequestHeadersSpec<*>): List<T> =
        requestRaw(requestObj)
            .getResultFromResponse(List::class.java)
            ?.mapNotNull { valueInList -> valueInList?.convertValue(clazz) }
            ?: listOf()

    /**
     * Generic аналог [requestList].
     */
    suspend inline fun <reified T : Any> requestList(requestObj: WebClient.RequestHeadersSpec<*>): List<T> =
        requestList(T::class.java, requestObj)

    /**
     * Запрос из ответа по [requestObj] объекта аналогично [request] и [Response.pagination].
     * В результате получаем список объектов [clazz]
     */
    suspend fun <T : Any> requestWithPagination(
        clazz: Class<T>,
        requestObj: WebClient.RequestHeadersSpec<*>
    ): Pair<List<T>, Pagination> =
        requestRaw(requestObj)
            .let {
                val convertedResultToList = it.getResultFromResponse(List::class.java)
                    ?.mapNotNull { valueInList -> valueInList?.convertValue(clazz) }
                (convertedResultToList ?: listOf()) to (it.pagination ?: Pagination(0, 0, 0, null, null))
            }

    /**
     * Generic аналог [requestWithPagination].
     */
    suspend inline fun <reified T : Any> requestWithPagination(requestObj: WebClient.RequestHeadersSpec<*>): Pair<List<T>, Pagination> =
        requestWithPagination(T::class.java, requestObj)

    /**
     * Запрос ответа по [requestObj] в исходном виде [Response].
     * Возбуждается исключение [ExtremumApiException],
     * если получен неуспешный статус заброса (не [HttpStatus.OK] или [HttpStatus.NOT_FOUND]).
     */
    suspend fun requestRaw(requestObj: WebClient.RequestHeadersSpec<*>): Response =
        requestObj
            .awaitExchange { response ->
                val statusCode = response.statusCode()
                logger.info("Response code: $statusCode")
                val body = response.awaitBody<Response>()

                if (body.code !in NOT_FAILED_STATUSES_VALUES) {
                    logger.warning("Error status: $statusCode")
                    if (body.alerts.isNotEmpty()) {
                        logger.warning("Alerts from response body: ${body.alerts}")
                        throw ExtremumApiException(
                            status = statusCode,
                            message = body.alerts.joinToString { it.code + ": " + it.message }
                        )
                    }
                    throw ExtremumApiException(code = body.code, message = "request failed.")
                }

                if (statusCode !in NOT_FAILED_STATUSES) {
                    throw ExtremumApiException(status = statusCode, message = "request failed.")
                }

                body
            }

    private fun <T : Any> Response.getResultFromResponse(clazz: Class<T>): T? {
        return this.result?.let { result ->
            result.convertValueSafe(clazz)
                ?: throw ExtremumApiException("Incorrect result type in response: $result. Expected ${clazz.simpleName}")
        }
    }

    val NOT_FAILED_STATUSES: List<HttpStatus> = listOf(
        HttpStatus.OK,
        HttpStatus.NOT_FOUND,
    )
    val NOT_FAILED_STATUSES_VALUES: List<Int> = NOT_FAILED_STATUSES.map { it.value() }
}