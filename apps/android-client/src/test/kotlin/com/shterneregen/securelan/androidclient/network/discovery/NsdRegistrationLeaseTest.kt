package com.shterneregen.securelan.androidclient.network.discovery

import org.junit.Assert.*
import org.junit.Test

class NsdRegistrationLeaseTest {
    @Test fun lateSuccessAfterStopUnregistersExactlyOnceWithoutReportingReady() {
        repeat(100) {
            var removed = 0; var ready = 0
            val lease = NsdRegistrationLease({ removed++ }, { ready++ })
            lease.close(); lease.close(); lease.registered(); lease.close()
            assertEquals(1, removed)
            assertEquals(0, ready)
        }
    }
    @Test fun successfulRegistrationHasOneOwnedRelease() {
        var removed = 0; var ready = 0
        val lease = NsdRegistrationLease({ removed++ }, { ready++ })
        lease.registered(); lease.close(); lease.close()
        assertEquals(1, ready)
        assertEquals(1, removed)
    }
}
