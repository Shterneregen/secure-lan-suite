package com.shterneregen.securelan.stego.service

import com.shterneregen.securelan.stego.model.BmpCapacity
import com.shterneregen.securelan.stego.model.ExtractedStegoPayload
import com.shterneregen.securelan.stego.model.StegoContentType

/**
 * Service for embedding and extracting plain and password-encrypted payloads within carrier BMP images.
 */
interface SteganographyService {
    /**
     * Inspects a BMP image and calculates its capacity and structure metrics.
     *
     * @param bmpBytes The raw bytes of the BMP image.
     * @return A [BmpCapacity] descriptor containing image dimensions and payload capacity.
     * @throws IllegalArgumentException If the image is not a valid or supported BMP file.
     */
    fun inspect(bmpBytes: ByteArray): BmpCapacity

    /**
     * Embeds a payload with the specified [StegoContentType] into a BMP image using LSB steganography.
     *
     * @param bmpBytes The carrier BMP image bytes.
     * @param payload The raw payload bytes to embed.
     * @param contentType The type descriptor of the payload.
     * @return A new byte array containing the BMP image with embedded data.
     * @throws IllegalArgumentException If the payload exceeds the image's capacity or the BMP format is invalid.
     */
    fun hide(bmpBytes: ByteArray, payload: ByteArray, contentType: StegoContentType): ByteArray

    /**
     * Extracts an embedded payload and its metadata from a steganographic BMP image.
     *
     * @param bmpBytes The BMP image bytes containing hidden data.
     * @return An [ExtractedStegoPayload] instance containing the payload, type, and encryption flag.
     * @throws IllegalArgumentException If no valid SecureLanSuite header is found or the data is corrupted.
     */
    fun extract(bmpBytes: ByteArray): ExtractedStegoPayload

    /**
     * Embeds a binary payload into a BMP image.
     *
     * @param bmpBytes The carrier BMP image bytes.
     * @param payload The binary payload bytes to embed.
     * @return A new byte array containing the BMP image with embedded data.
     * @throws IllegalArgumentException If the payload exceeds capacity or the BMP format is invalid.
     */
    fun hidePayload(bmpBytes: ByteArray, payload: ByteArray): ByteArray = hide(bmpBytes, payload, StegoContentType.BINARY)

    /**
     * Extracts a plain (unencrypted) binary payload from a BMP image.
     *
     * @param bmpBytes The BMP image bytes containing hidden binary data.
     * @return The extracted binary payload bytes.
     * @throws IllegalStateException If the embedded payload is marked as encrypted.
     * @throws IllegalArgumentException If no valid steganography payload is present.
     */
    fun extractPayload(bmpBytes: ByteArray): ByteArray

    /**
     * Embeds a UTF-8 text message into a BMP image.
     *
     * @param bmpBytes The carrier BMP image bytes.
     * @param message The text message to embed.
     * @return A new byte array containing the BMP image with embedded text.
     * @throws IllegalArgumentException If the message exceeds capacity or the BMP format is invalid.
     */
    fun hideText(bmpBytes: ByteArray, message: String): ByteArray

    /**
     * Extracts an unencrypted UTF-8 text message from a BMP image.
     *
     * @param bmpBytes The BMP image bytes containing hidden text.
     * @return The extracted text message.
     * @throws IllegalStateException If the payload is encrypted or is not UTF-8 text.
     * @throws IllegalArgumentException If no valid steganography payload is present.
     */
    fun extractText(bmpBytes: ByteArray): String

    /**
     * Encrypts a binary payload with the provided password and embeds it into a BMP image.
     *
     * @param bmpBytes The carrier BMP image bytes.
     * @param payload The plain binary payload to encrypt and embed.
     * @param password The password character array used for encryption.
     * @return A new byte array containing the BMP image with encrypted data embedded.
     * @throws IllegalArgumentException If the password is empty or payload exceeds capacity.
     */
    fun hideEncryptedPayload(bmpBytes: ByteArray, payload: ByteArray, password: CharArray): ByteArray

    /**
     * Extracts and decrypts an encrypted binary payload from a BMP image using the given password.
     *
     * @param bmpBytes The BMP image bytes containing encrypted hidden data.
     * @param password The password character array used for decryption.
     * @return The decrypted binary payload bytes.
     * @throws IllegalStateException If the payload is unencrypted or decryption fails due to wrong password.
     * @throws IllegalArgumentException If the password is empty or data is invalid.
     */
    fun extractEncryptedPayload(bmpBytes: ByteArray, password: CharArray): ByteArray

    /**
     * Encrypts a UTF-8 text message with the provided password and embeds it into a BMP image.
     *
     * @param bmpBytes The carrier BMP image bytes.
     * @param message The text message to encrypt and embed.
     * @param password The password character array used for encryption.
     * @return A new byte array containing the BMP image with encrypted text embedded.
     * @throws IllegalArgumentException If the password is empty or payload exceeds capacity.
     */
    fun hideEncryptedText(bmpBytes: ByteArray, message: String, password: CharArray): ByteArray

    /**
     * Extracts and decrypts an encrypted UTF-8 text message from a BMP image using the given password.
     *
     * @param bmpBytes The BMP image bytes containing encrypted hidden text.
     * @param password The password character array used for decryption.
     * @return The decrypted text message.
     * @throws IllegalStateException If the payload is unencrypted, wrong content type, or wrong password.
     * @throws IllegalArgumentException If the password is empty or data is invalid.
     */
    fun extractEncryptedText(bmpBytes: ByteArray, password: CharArray): String
}
