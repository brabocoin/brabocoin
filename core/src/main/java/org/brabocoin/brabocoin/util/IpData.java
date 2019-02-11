package org.brabocoin.brabocoin.util;

public class IpData {
    private String deviceName;
    private String address;
    private String hostname;


    public IpData(String deviceName, String address, String hostname) {
        this.deviceName = deviceName;
        this.address = address;
        this.hostname = hostname;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getAddress() {
        return address;
    }

    public String getHostname() {
        return hostname;
    }
}
