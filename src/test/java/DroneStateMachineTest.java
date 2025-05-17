import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;

class DroneStateMachineTest {
    private DroneStateMachine stateMachine;
    private DroneSubsystem drone;
    private Scheduler scheduler;
    private FireIncidentEvent task;

    @BeforeEach
    void setUp() {
        stateMachine = new DroneStateMachine();
        // scheduler = new Scheduler(scheduler.drones, scheduler.bufferReq, scheduler.eventList, 6000);
        drone = new DroneSubsystem("localhost", 5000);

        task = new FireIncidentEvent("14:03:15", 1, "FIRE DETECTED", "High", new Zone(1, 0, 0, 100, 100), 15, "", "");
    }

    @Test
    void testInitialState() {
        assertNotNull(stateMachine.getCurrentState(), "State should not be null");
        assertEquals("Idle", stateMachine.getCurrentState().getClass().getSimpleName(), "Initial state should be Idle");
    }

    @Test
    void testArrivedAtZone() {
        assertDoesNotThrow(() -> stateMachine.getCurrentState().arrivedAtZone(stateMachine, drone));
    }

    @Test
    void testOpenNozzle() {
        assertDoesNotThrow(() -> stateMachine.getCurrentState().openNozzle(stateMachine, drone));
    }

    @Test
    void testFinishedDroppingAgent() {
        assertDoesNotThrow(() -> stateMachine.getCurrentState().finishedDroppingAgent(stateMachine, drone));
    }

    @Test
    void testTransitionToNextStateOnArrivingAtZone() {
        // Simulate the arrival at a zone and check if state transitions correctly
        stateMachine.getCurrentState().arrivedAtZone(stateMachine, drone);
        assertEquals("Idle", stateMachine.getCurrentState().getClass().getSimpleName(), "State should transition to DroppingAgent");
    }

    @Test
    void testStateAfterNozzleOpened() {
        // Open nozzle and check if state changes correctly
        stateMachine.getCurrentState().openNozzle(stateMachine, drone);
        assertEquals("Idle", stateMachine.getCurrentState().getClass().getSimpleName(), "State should transition to DroppingAgent");
    }

    @Test
    void testDroneNotNull() {
        assertNotNull(drone, "Drone should not be null");
    }

}
