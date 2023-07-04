package io.extremum.model.tools.api

import org.springframework.http.HttpStatus

class ExtremumApiException(message: String) : RuntimeException(message) {
    constructor(code: Int, message: String) : this(message = "$code: $message")
    constructor(status: HttpStatus, message: String) : this(message = "${status.value()}: $message")
}