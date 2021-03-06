package org.brabocoin.brabocoin.node;

import org.brabocoin.brabocoin.exceptions.MalformedSocketException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class PeerTest {

    /**
     * Creates a valid peer using a valid socket string.
     */
    @Test
    void validPeerInstantiationString() throws MalformedSocketException {
        Peer p = new Peer("hatseflats:666");
        assertNotNull(p);
        p.shutdown();
    }

    /**
     * Creates a valid peer using an instantiated.
     */
    @Test
    void validPeerInstantiationInetSocketAddress() throws MalformedSocketException,
                                                          UnknownHostException {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost(), 1);
        Peer p = new Peer(address);
        assertNotNull(p);
        p.shutdown();
    }

    /**
     * Tests for a MalformedSocketException on peer instantiation when passing a invalid number.
     */
    @Test()
    void invalidPeerInstantiationNoNumber() {
        assertThrows(MalformedSocketException.class, () -> new Peer("hatseflats:NAN"));
    }

    /**
     * Tests for a MalformedSocketException on peer instantiation when passing a string with no
     * colon.
     */
    @Test()
    void invalidPeerInstantiationNoColon() {
        assertThrows(MalformedSocketException.class, () -> new Peer("hatseflats"));
    }

    /**
     * Tests for a MalformedSocketException on peer instantiation when passing a string with
     * multiple colons.
     */
    @Test()
    void invalidPeerInstantiationMultipleColons() {
        assertThrows(MalformedSocketException.class, () -> new Peer("hatseflats:bla:123"));
    }

    /**
     * Tests for a MalformedSocketException on peer instantiation when passing an invalid hostname.
     */
    @Test()
    void invalidPeerInstantiationNoHostname() {
        assertThrows(MalformedSocketException.class, () -> new Peer("----:123"));
    }

    /**
     * Tests for a MalformedSocketException on peer instantiation when passing an invalid hostname.
     */
    @Test()
    void invalidPeerInstantiationInvalidHostname() {
        assertThrows(MalformedSocketException.class, () -> new Peer("hatse$#flats:123"));
    }

    /**
     * Tests whether the shutdown method closes the peer channel.
     */
    @Test()
    void peerStop() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");
        p.shutdown();
        assertFalse(p.isRunning());
    }

    /**
     * Tests the blocking stub getter.
     */
    @Test()
    void getBlockingStub() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");

        assertNotNull(p.getBlockingStub());

        p.shutdown();
    }

    /**
     * Tests the async stub getter.
     */
    @Test()
    void getAsyncStub() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");

        assertNotNull(p.getAsyncStub());

        p.shutdown();
    }

    /**
     * Tests the toString method of a peer.
     */
    @Test()
    void toStringTest() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");

        assertTrue(p.toString().matches("\\w+:\\d+ \\(running\\)"));

        p.shutdown();
    }

    /**
     * Test equals method for same object.
     */
    @Test
    void equalsSameObject() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");

        assertEquals(p, p);

        p.shutdown();
    }

    /**
     * Test equals method for same hostname and port.
     */
    @Test
    void equalsSameSocket() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");
        Peer p2 = new Peer("localhost:123");

        assertEquals(p, p2);

        p.shutdown();
        p2.shutdown();
    }

    /**
     * Test equals method for same hostname and port.
     */
    @Test
    void equalsDiffSocketHostname() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");
        Peer p2 = new Peer("bladiebla:123");

        assertNotEquals(p, p2);

        p.shutdown();
        p2.shutdown();
    }

    /**
     * Test equals method for same hostname and port.
     */
    @Test
    void equalsDiffSocketPort() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");
        Peer p2 = new Peer("localhost:145");

        assertNotEquals(p, p2);

        p.shutdown();
        p2.shutdown();
    }

    /**
     * Test equals method for {@code null}.
     */
    @Test
    void equalsNull() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");

        assertNotEquals(null, p);

        p.shutdown();
    }

    /**
     * Test equals method for different object.
     */
    @Test
    void equalsDiffObject() throws MalformedSocketException {
        Peer p = new Peer("localhost:123");
        Object o = new Object();

        assertNotEquals(p, o);

        p.shutdown();
    }
}
