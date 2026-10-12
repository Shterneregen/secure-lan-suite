package com.shterneregen.securelan.filetransfer.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FileTransferMetadataTest {
    @Test
    fun shouldCompactSerializeAndDeserialize() {
        val original = FileTransferMetadata(
            transferId = "transfer-123",
            senderId = "alice",
            recipientId = "bob",
            fileName = "notes.txt",
            fileSize = 1024L,
        )

        val serialized = original.compactSerialize()
        val deserialized = FileTransferMetadata.deserializeCompact(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun shouldCompactSerializeAndDeserializeWithSpecialCharacters() {
        val original = FileTransferMetadata(
            transferId = "uuid-with|pipe:and#symbols",
            senderId = "sender/with/slashes",
            recipientId = "recipient@lan",
            fileName = "Отчёт 2026! @#%^&().pdf",
            fileSize = 999_999L,
        )

        val serialized = original.compactSerialize()
        val deserialized = FileTransferMetadata.deserializeCompact(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun shouldSerializeAndDeserializeLegacyFormat() {
        val original = FileTransferMetadata(
            transferId = "legacy-1",
            senderId = "user1",
            recipientId = "user2",
            fileName = "legacy.bin",
            fileSize = 42L,
        )

        val serialized = original.serialize()
        val deserialized = FileTransferMetadata.deserialize(serialized)

        assertEquals(original, deserialized)
    }

    @Test
    fun shouldRejectBlankFileNameOrNegativeSize() {
        assertThrows(IllegalArgumentException::class.java) {
            FileTransferMetadata("id", "sender", "recipient", "   ", 100L)
        }

        assertThrows(IllegalArgumentException::class.java) {
            FileTransferMetadata("id", "sender", "recipient", "file.txt", -1L)
        }
    }

    @Test
    fun shouldRejectMalformedPayloads() {
        assertThrows(IllegalArgumentException::class.java) {
            FileTransferMetadata.deserializeCompact("not|enough|parts")
        }

        assertThrows(IllegalArgumentException::class.java) {
            FileTransferMetadata.deserializeCompact("a|b|c|d|not-a-number")
        }

        assertThrows(IllegalArgumentException::class.java) {
            FileTransferMetadata.deserialize("line1\nline2")
        }
    }
}
