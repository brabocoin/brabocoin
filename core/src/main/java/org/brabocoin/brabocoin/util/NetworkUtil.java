package org.brabocoin.brabocoin.util;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.StringJoiner;

public class NetworkUtil {

    public static List<IpData> getIpData() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        List<IpData> ipData = new ArrayList<>();

        for (NetworkInterface networkInterface : Collections.list(interfaces)) {
            StringJoiner addressStringJoiner = new StringJoiner(", ");
            StringJoiner hostnameStringJoiner = new StringJoiner(", ");
            for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                if (address.isLoopbackAddress()
                    || address.isMulticastAddress()
                    || address.isAnyLocalAddress()
                    || address instanceof Inet6Address) {
                    continue;
                }
                addressStringJoiner.add(address.getHostAddress());
                hostnameStringJoiner.add(address.getHostName());
            }

            if (!addressStringJoiner.toString().equals("")) {
                ipData.add(new IpData(
                    networkInterface.getDisplayName(),
                    addressStringJoiner.toString(),
                    hostnameStringJoiner.toString()
                ));
            }
        }

        return ipData;
    }
}
