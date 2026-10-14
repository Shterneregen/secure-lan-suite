package com.shterneregen.securelan.crypto.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SecureProtocolTest {
    private fun exchange(clientPassword: String = "test-password", serverPassword: String = "test-password", clientContext: String = "chat:v2:alice", serverContext: String = clientContext): Pair<SecureSessionKeys, SecureSessionKeys> {
        val toClient = ArrayBlockingQueue<String>(4)
        val toServer = ArrayBlockingQueue<String>(4)
        fun receive(queue: ArrayBlockingQueue<String>): String = queue.poll(2, TimeUnit.SECONDS) ?: throw IOException("Authentication closed")
        val worker = Executors.newSingleThreadExecutor()
        try {
            val server = worker.submit<SecureSessionKeys> {
                PasswordAuthenticatedHandshake.server(serverPassword, serverContext, { toClient.put(it) }, { receive(toServer) })
            }
            val client = PasswordAuthenticatedHandshake.client(clientPassword, clientContext, { toServer.put(it) }, { receive(toClient) })
            return client to server.get(2, TimeUnit.SECONDS)
        } finally { worker.shutdownNow() }
    }

    @Test fun mutualAuthenticationAndDirectionalRecords() {
        val (clientKeys, serverKeys) = exchange()
        assertArrayEquals(clientKeys.binding(), serverKeys.binding())
        val client = SecureRecordCipher(clientKeys, true)
        val server = SecureRecordCipher(serverKeys, false)
        assertArrayEquals("hello".toByteArray(), server.decrypt(client.encrypt("hello".toByteArray())))
        assertArrayEquals("answer".toByteArray(), client.decrypt(server.encrypt("answer".toByteArray())))
        assertArrayEquals(ByteArray(0), server.decrypt(client.encrypt(ByteArray(0))))
    }

    @Test fun wrongPasswordAndChangedIdentityCannotAuthenticate() {
        assertThrows(IOException::class.java) { exchange(clientPassword = "wrong") }
        assertThrows(IOException::class.java) { exchange(clientContext = "file:v2", serverContext = "chat:v2") }
        assertThrows(IOException::class.java) { exchange(clientContext = "alice:changed-public-key", serverContext = "alice:original-public-key") }
    }

    @Test fun fakeServerAndInvalidPublicValuesFailClosed() {
        assertThrows(IOException::class.java) {
            PasswordAuthenticatedHandshake.client("password", "chat:v2", {}, { "invalid-rsa-key" })
        }
        val salt = java.util.Base64.getEncoder().encodeToString(ByteArray(32))
        assertThrows(IOException::class.java) {
            PasswordAuthenticatedHandshake.client("password", "chat:v2", {}, { "$salt:0" })
        }
    }

    @Test fun replayIsRejectedAndPermanentlyPoisonsReader() {
        val (a, b) = exchange()
        val client = SecureRecordCipher(a, true)
        val server = SecureRecordCipher(b, false)
        val record = client.encrypt("message".toByteArray())
        server.decrypt(record)
        assertThrows(IOException::class.java) { server.decrypt(record) }
        assertThrows(IOException::class.java) { server.decrypt(client.encrypt("later".toByteArray())) }
    }

    @Test fun reorderingReflectionTamperingAndCrossSessionRecordsAreRejected() {
        val (a, b) = exchange()
        val client = SecureRecordCipher(a, true)
        val first = client.encrypt("AAAA".toByteArray())
        val second = client.encrypt("BBBB".toByteArray())
        assertThrows(IOException::class.java) { SecureRecordCipher(b, false).decrypt(second) }
        assertThrows(IOException::class.java) { SecureRecordCipher(a, true).decrypt(first) }
        val tampered = first.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertThrows(IOException::class.java) { SecureRecordCipher(b, false).decrypt(tampered) }
        val (_, other) = exchange()
        assertThrows(IOException::class.java) { SecureRecordCipher(other, false).decrypt(first) }
        assertFalse(first.contentEquals(second))
    }

    @Test fun oversizedRecordAndWrongPeerSignatureAreRejected() {
        val (a, _) = exchange()
        assertThrows(IOException::class.java) { SecureRecordCipher(a, true).encrypt(ByteArray(SecureRecordCipher.MAX_PLAINTEXT_BYTES + 1)) }
        val alice = PeerIdentity()
        val mallory = PeerIdentity()
        val payload = "bound-transfer".toByteArray()
        PeerIdentity.verify(alice.publicKey, payload, alice.sign(payload))
        assertThrows(IOException::class.java) { PeerIdentity.verify(alice.publicKey, payload, mallory.sign(payload)) }
        assertThrows(IOException::class.java) { PeerIdentity.verify(alice.publicKey, "changed".toByteArray(), alice.sign(payload)) }
    }
}
