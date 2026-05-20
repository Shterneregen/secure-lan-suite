package com.shterneregen.securelan.stego

class StegoException : RuntimeException {
    constructor(message: String) : super(message)

    constructor(message: String, cause: Throwable) : super(message, cause)
}
