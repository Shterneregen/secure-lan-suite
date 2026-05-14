package com.shterneregen.securelan.common.net.udp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

public final class BroadcastAddressResolver {
    private final NetworkInterfaceProvider networkInterfaceProvider;

    public BroadcastAddressResolver() {
        this(NetworkInterfaceProvider.systemDefault());
    }

    public BroadcastAddressResolver(NetworkInterfaceProvider networkInterfaceProvider) {
        this.networkInterfaceProvider = Objects.requireNonNull(networkInterfaceProvider, "networkInterfaceProvider");
    }

    public List<InetAddress> resolve() throws SocketException, UnknownHostException {
        List<InetAddress> result = new ArrayList<>();
        result.add(InetAddress.getLoopbackAddress());
        result.add(InetAddress.getByName("255.255.255.255"));
        Enumeration<NetworkInterface> interfaces = networkInterfaceProvider.networkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }
            networkInterface.getInterfaceAddresses().stream()
                    .filter(BroadcastAddressResolver::isIpv4Address)
                    .map(InterfaceAddress::getBroadcast)
                    .filter(Objects::nonNull)
                    .forEach(result::add);
        }
        return result.stream().distinct().toList();
    }

    private static boolean isIpv4Address(InterfaceAddress address) {
        return address.getAddress() instanceof Inet4Address;
    }

    @FunctionalInterface
    public interface NetworkInterfaceProvider {
        Enumeration<NetworkInterface> networkInterfaces() throws SocketException;

        static NetworkInterfaceProvider systemDefault() {
            return NetworkInterface::getNetworkInterfaces;
        }
    }
}
