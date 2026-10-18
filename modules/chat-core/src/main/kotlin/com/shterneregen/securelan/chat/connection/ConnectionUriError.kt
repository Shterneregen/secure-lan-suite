package com.shterneregen.securelan.chat.connection

enum class ConnectionUriError {
    InputTooLong, InvalidUri, WrongScheme, UnsupportedVersion, UnsupportedProtocol, MissingField, DuplicateField,
    UnknownField, InvalidAddress, UnsupportedAddressScope, InvalidPort, InvalidText,
}
sealed interface ConnectionUriParseResult {
    data class Success(val descriptor: RoomConnectionDescriptor) : ConnectionUriParseResult
    data class Invalid(val error: ConnectionUriError, val field: String? = null) : ConnectionUriParseResult
}
