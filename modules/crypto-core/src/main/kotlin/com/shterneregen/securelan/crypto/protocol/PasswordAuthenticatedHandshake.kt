package com.shterneregen.securelan.crypto.protocol

import org.bouncycastle.crypto.agreement.srp.SRP6Client
import org.bouncycastle.crypto.agreement.srp.SRP6Server
import org.bouncycastle.crypto.agreement.srp.SRP6StandardGroups
import org.bouncycastle.crypto.agreement.srp.SRP6VerifierGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom
import java.util.Base64

/**
 * Mutually authenticated SRP-6a exchange using the fixed RFC 5054 3072-bit group and SHA-256.
 * The password never crosses the transport. The protocol/identity context participates in the
 * verifier and key derivation. Each invocation owns its Bouncy Castle state and fresh ephemeral
 * values; untrusted peers cannot choose group parameters. Callers bound IO and reject old versions.
 */
object PasswordAuthenticatedHandshake {
    private val group = SRP6StandardGroups.rfc5054_3072
    private val random = SecureRandom()

    @JvmStatic
    fun client(password: String, context: String, send: (String) -> Unit, receive: () -> String): SecureSessionKeys =
        authenticate {
            val challenge = receive().split(':')
            if (challenge.size != 2) throw IOException("Invalid authentication challenge")
            val salt = Base64.getDecoder().decode(challenge[0])
            if (salt.size != 32) throw IOException("Invalid authentication salt")
            val serverB = number(challenge[1], 768)
            val client = SRP6Client().apply { init(group, SHA256Digest(), random) }
            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            val clientA = try {
                client.generateClientCredentials(salt, context.toByteArray(Charsets.UTF_8), passwordBytes)
            } finally { passwordBytes.fill(0) }
            client.calculateSecret(serverB)
            val proof = client.calculateClientEvidenceMessage()
            send("${clientA.toString(16)}:${proof.toString(16)}")
            if (!client.verifyServerEvidenceMessage(number(receive(), 64))) {
                throw IOException("Authentication failed")
            }
            SecureSessionKeys.derive(client.calculateSessionKey(), context, salt, clientA, serverB)
        }

    @JvmStatic
    fun server(password: String, context: String, send: (String) -> Unit, receive: () -> String): SecureSessionKeys =
        authenticate {
            val salt = ByteArray(32).also(random::nextBytes)
            val generator = SRP6VerifierGenerator().apply { init(group, SHA256Digest()) }
            val passwordBytes = password.toByteArray(Charsets.UTF_8)
            val verifier = try {
                generator.generateVerifier(salt, context.toByteArray(Charsets.UTF_8), passwordBytes)
            } finally { passwordBytes.fill(0) }
            val server = SRP6Server().apply { init(group, verifier, SHA256Digest(), random) }
            val serverB = server.generateServerCredentials()
            send("${Base64.getEncoder().encodeToString(salt)}:${serverB.toString(16)}")
            val response = receive().split(':')
            if (response.size != 2) throw IOException("Invalid authentication response")
            val clientA = number(response[0], 768)
            server.calculateSecret(clientA)
            if (!server.verifyClientEvidenceMessage(number(response[1], 64))) {
                throw IOException("Authentication failed")
            }
            send(server.calculateServerEvidenceMessage().toString(16))
            SecureSessionKeys.derive(server.calculateSessionKey(), context, salt, clientA, serverB)
        }

    private fun number(value: String, maxLength: Int): BigInteger {
        if (value.isEmpty() || value.length > maxLength || !value.all { it in '0'..'9' || it in 'a'..'f' }) {
            throw IOException("Invalid authentication value")
        }
        return BigInteger(value, 16)
    }

    private fun <T> authenticate(action: () -> T): T = try {
        action()
    } catch (error: IOException) {
        throw error
    } catch (error: Exception) {
        throw IOException("Authentication failed", error)
    }
}
