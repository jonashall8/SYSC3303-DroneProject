import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.Queue;

public class SchedulerTest {

    private Scheduler scheduler;
    DatagramSocket socket = new DatagramSocket();
    DroneRequestBuffer bufferReq;

    public SchedulerTest() throws SocketException {
    }

    @BeforeEach
    void setUp() throws SocketException {
        FireEventList eventList = new FireEventList();
        DroneRequestBuffer buffer = new DroneRequestBuffer();
        DroneFleet drones = new DroneFleet(eventList);
        InProgressEvents ipe = new InProgressEvents();
        scheduler = new Scheduler(drones, buffer, eventList, ipe, 4000);
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.stop();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Ensure socket is closed after each test to avoid address binding issues
        }
    }

    @Test
    void testSchedulerInitialState() {
        assertNotNull(scheduler);
        assertEquals(0, scheduler.getDrones().size());
        assertEquals(0, scheduler.eventList.getListSize());
    }

    @Test
    void testDroneRegistration() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            String response = scheduler.handleInitializeDrone(address, 6000);
            int droneID = Integer.parseInt(response);
            Map<Integer, DroneData> drones = scheduler.getDrones();
            assertTrue(drones.containsKey(droneID));
            assertEquals(droneID, drones.get(droneID).getDroneID());
        } catch (IOException e) {
            fail("IOException thrown: " + e.getMessage());
        }
    }

    @Test
    void testFireEventHandling() {
        try {
            String[] eventParts = new String[3];
            eventParts[0] = "14";
            eventParts[1] = "03";
            eventParts[2] = "15,1,FIRE_DETECTED,High,30,New,NO_FAULT,N/A,1,0,0,700,600";
            InetAddress address = InetAddress.getLocalHost();
            String response = scheduler.handleFireEvent(eventParts, address, 5000);

            FireEventList fireEvents = scheduler.eventList;
            assertEquals(1, fireEvents.getListSize());
            assertEquals("FIRE_EVENT:RECEIVED", response);
        } catch (IOException e) {
            fail("IOException thrown: " + e.getMessage());
        }
    }



    @Test
    void testDroneAssignment() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            scheduler.handleInitializeDrone(address, 6000);
            DroneData drone = scheduler.getDrones().values().iterator().next();
            drone.setHasRequested(true);

            FireIncidentEvent fireEvent = new FireIncidentEvent("12:34:56", 1, "FIRE", "HIGH", new Zone(1, 10, 10, 20, 20), 100, "", "");
            String[] parts = fireEvent.toString().split(":");
            scheduler.handleFireEvent(parts,InetAddress.getLocalHost(),5000);

            String task = scheduler.assignTaskToDrone();

            assertFalse(task.isEmpty());
            assertTrue(task.contains(":"));
        } catch (IOException e) {
            fail("IOException thrown: " + e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testHandleStuckDrones() throws InterruptedException {
        int droneId = 1;
        Point location = new Point(10, 10);
        int remainingPayload = 100;
        InetAddress address = InetAddress.getLoopbackAddress();
        int port = 6000;

        DroneData lostDrone = new DroneData(droneId, location, remainingPayload, address, port);
        lostDrone.isLost = true;
        lostDrone.droneJobStatus = -1;
        lostDrone.setTravelTime(1000);

        scheduler.getDrones().put(droneId, lostDrone);

        FireIncidentEvent fireEvent = new FireIncidentEvent("12:00:00", droneId, "FIRE", "HIGH", new Zone(1, 10, 10, 20, 20), 50, "", "");
        scheduler.inProgressEvents.addInProgressEvent(droneId, fireEvent);

        scheduler.HandleStuckDrones();

        assertFalse(scheduler.getDrones().containsKey(droneId), "Drone should be removed from fleet");
        assertNull(scheduler.inProgressEvents.getAnEventInProgress(droneId), "In-progress event should be removed");
        assertEquals(1, scheduler.eventList.getListSize(), "Event should be re-added to event list");

        FireIncidentEvent requeuedEvent = scheduler.eventList.getList().peek();
        assertEquals("NO_FAULT", requeuedEvent.getFault());
        assertEquals("N/A", requeuedEvent.getFaultType());
    }
    @Test
    void testFaultHandlingCorruptedMessage() {
        try {
            tearDown();
            DroneRequestBuffer bufferReq = new DroneRequestBuffer();
            FireEventList events = new FireEventList();
            InProgressEvents inProgressEvents = new InProgressEvents();
            Thread droneListener = new Thread(new Scheduler(new DroneFleet(events), bufferReq, events, inProgressEvents, 5555), "Drone listener");
            droneListener.start();
            int port = 6000;
            // Create a DatagramSocket bound to the port
            DatagramSocket socket = new DatagramSocket(port);

            // Prepare data to send
            String message = "22:33:33";
            byte[] sendData = message.getBytes();
            InetAddress address = InetAddress.getByName("127.0.0.1");
            // Send packet to self
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, 5555);
            socket.send(sendPacket);
            System.out.println("Sent: " + message);

            // Receive the packet
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            // Process received data
            String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Received: " + receivedMessage);
            assertEquals( "CORRUPTED_MESSAGED", receivedMessage);

            // Close the socket
            socket.close();
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
}
