import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubSystemTest {

    private FireIncidentSubSystem fireSystem;

    @BeforeEach
    void setUp() {
        fireSystem = new FireIncidentSubSystem("src/test/resources/sample_zone_file.csv", "src/test/resources/Sample_event_file.csv");
    }

    @Test
    void testLoadZones() throws Exception {
        Field zonesField = FireIncidentSubSystem.class.getDeclaredField("zones");
        zonesField.setAccessible(true);
        Map<Integer, Zone> zones = (Map<Integer, Zone>) zonesField.get(fireSystem);

        assertNotNull(zones);
        assertTrue(zones.size() > 0, "Zones should be loaded");
    }

    @Test
    void testLoadFireIncidents() throws Exception {
        Field fireIncidentsField = FireIncidentSubSystem.class.getDeclaredField("fireIncidents");
        fireIncidentsField.setAccessible(true);
        List<FireIncidentEvent> incidents = (List<FireIncidentEvent>) fireIncidentsField.get(fireSystem);

        assertNotNull(incidents);
        assertFalse(incidents.isEmpty(), "Fire incidents should be loaded");
    }

//    @Test
//    void testProcessFireIncidents() throws Exception {
//        fireSystem.processFireIncidents();
//
//        Field fireIncidentsField = FireIncidentSubSystem.class.getDeclaredField("fireIncidents");
//        fireIncidentsField.setAccessible(true);
//        List<FireIncidentEvent> incidents = (List<FireIncidentEvent>) fireIncidentsField.get(fireSystem);
//
//        assertFalse(incidents.isEmpty(), "Fire incidents should be processed");
//    }

    @Test
    void testSendRequest() throws Exception {
        String testMessage = "Test Fire Incident";
        InetAddress localhost = InetAddress.getByName("127.0.0.1");
        int testPort = 9876;

        try (DatagramSocket testSocket = new DatagramSocket(testPort)) {
            testSocket.setSoTimeout(2000); // Set timeout to avoid infinite wait

            Thread listener = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                    testSocket.receive(receivedPacket);

                    String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                    assertEquals(testMessage, receivedMessage);
                    assertEquals(localhost, receivedPacket.getAddress());
                } catch (Exception e) {

                }
            });

            listener.start();

            // Invoke the private sendRequest method
            Method sendRequestMethod = FireIncidentSubSystem.class.getDeclaredMethod("sendRequest", String.class);
            sendRequestMethod.setAccessible(true);
            sendRequestMethod.invoke(fireSystem, testMessage);

            listener.join(); // Wait for listener to finish
        }
    }
}
