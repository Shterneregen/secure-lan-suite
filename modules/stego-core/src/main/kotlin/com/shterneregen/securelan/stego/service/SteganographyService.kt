package com.shterneregen.securelan.stego.service

import com.shterneregen.securelan.stego.model.BmpCapacity
import com.shterneregen.securelan.stego.model.ExtractedStegoPayload
import com.shterneregen.securelan.stego.model.StegoContentType

interface SteganographyService {
    fun inspect(bmpBytes: ByteArray): BmpCapacity

    fun hide(bmpBytes: ByteArray, payload: ByteArray, contentType: StegoContentType): ByteArray

    fun extract(bmpBytes: ByteArray): ExtractedStegoPayload

    fun hidePayload(bmpBytes: ByteArray, payload: ByteArray): ByteArray = hide(bmpBytes, payload, StegoContentType.BINARY)

    fun extractPayload(bmpBytes: ByteArray): ByteArray

    fun hideText(bmpBytes: ByteArray, message: String): ByteArray

    fun extractText(bmpBytes: ByteArray): String

    fun hideEncryptedPayload(bmpBytes: ByteArray, payload: ByteArray, password: CharArray): ByteArray

    fun extractEncryptedPayload(bmpBytes: ByteArray, password: CharArray): ByteArray

    fun hideEncryptedText(bmpBytes: ByteArray, message: String, password: CharArray): ByteArray

    fun extractEncryptedText(bmpBytes: ByteArray, password: CharArray): String
}
