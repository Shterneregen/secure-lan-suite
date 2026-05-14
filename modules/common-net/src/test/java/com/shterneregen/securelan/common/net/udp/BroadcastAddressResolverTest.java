package com.shterneregen.securelan.common.net.udp;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BroadcastAddressResolverTest {
    @Test
    void shouldAlwaysIncludeLoopbackAndGlobalBroadcast() throws Exception {
        BroadcastAddressResolver resolver = new BroadcastAddressResolver(java.util.Collections::emptyEnumeration);

        var addresses = resolver.resolve();

        assertTrue(addresses.contains(InetAddress.getLoopbackAddress()));
        assertTrue(addresses.contains(InetAddress.getByName("255.255.255.255")));
    }
}
