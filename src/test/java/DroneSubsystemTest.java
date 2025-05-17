import org.junit.jupiter.api.*;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DroneSubsystemTest {

    DroneSubsystem drone;
    Point fireCenter1;
    Point fireCenter2;

    @BeforeEach
    void setUp() {
        // Given
        FireEventList eventList = new FireEventList();
        DroneRequestBuffer buffer = new DroneRequestBuffer();
        DroneFleet drones = new DroneFleet(eventList);
        InProgressEvents inProgressEvents = new InProgressEvents();

        Scheduler mockScheduler = new Scheduler(drones, buffer, eventList, inProgressEvents, 5000);
        drone = new DroneSubsystem("localhost", 5000);
        fireCenter1 = new Point(10, 0); // 10 units away on the x-axis
        fireCenter2 = new Point(200, 350);
    }

    @Test
    void testCalculateTravelTime() {
        // When
        double travelTime1 = drone.calculateTravelTime(fireCenter1);

        // Then
        // distance = 10, speed = 5.8 => expected time = 10 / 5.8 ~ 1.724...
        assertEquals(10 / 5.8, travelTime1, 1e-7);


        double travelTime2 = drone.calculateTravelTime(fireCenter2);
        assertEquals(403.112887 / 5.8, travelTime2, 1e-7);
    }

    @Test
    void testCalculateDropTime() {
        double waterNeeded1 = drone.calculateDropTime(10);

        assertEquals(10, waterNeeded1);

        double waterNeeded2 = drone.calculateDropTime(15);
        assertEquals(15.0, waterNeeded2);
    }

    @Test
    void testRefillPayload() {
        drone.refillPayload();
        assertEquals(15, drone.getRemainingPayload());
    }

    @Test
    void testSetAndGetStatus() {
        drone.setStatus("En Route");
        assertEquals("En Route", drone.getStatus());
    }

}

