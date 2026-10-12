package com.shterneregen.securelan.stego.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BmpCapacityTest {
    @Test
    fun shouldInstantiateValidCapacity() {
        val capacity = BmpCapacity(
            width = 100,
            height = 100,
            bitsPerPixel = 24,
            carrierBytes = 30000,
            headerBytes = 12,
            payloadCapacityBytes = 3738,
        )

        assertEquals(100, capacity.width)
        assertEquals(100, capacity.height)
        assertEquals(24, capacity.bitsPerPixel)
        assertEquals(30000, capacity.carrierBytes)
        assertEquals(12, capacity.headerBytes)
        assertEquals(3738, capacity.payloadCapacityBytes)
    }

    @Test
    fun shouldValidateNonPositiveDimensions() {
        assertThrows(IllegalArgumentException::class.java) {
            BmpCapacity(0, 100, 24, 100, 12, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BmpCapacity(100, -1, 24, 100, 12, 0)
        }
    }

    @Test
    fun shouldValidateBitsPerPixel() {
        assertThrows(IllegalArgumentException::class.java) {
            BmpCapacity(10, 10, 16, 100, 12, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BmpCapacity(10, 10, 8, 100, 12, 0)
        }
    }

    @Test
    fun shouldValidateHeaderAndCarrierBytes() {
        assertThrows(IllegalArgumentException::class.java) {
            BmpCapacity(10, 10, 24, -1, 12, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BmpCapacity(10, 10, 24, 100, 0, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BmpCapacity(10, 10, 24, 100, 12, -1)
        }
    }
}
