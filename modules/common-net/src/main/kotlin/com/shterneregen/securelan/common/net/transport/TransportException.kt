package com.shterneregen.securelan.common.net.transport

class TransportException : RuntimeException {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}
