package io.extremum.model.tools.api

import org.springframework.http.HttpStatusCode

class ExtremumApiException(message: String) : RuntimeException(message) {
    constructor(code: Int, message: String) : this(message = "$code: $message")
    constructor(code: HttpStatusCode, message: String) : this(message = "$code: $message")
}