import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**NEED TO MODIFY THIS FOR ITERATION 3
 * */
/*
public class SchedulerStateTest {

    private DroneSubsystem drone;
    private Scheduler scheduler;
    private FireIncidentEvent event;


    @BeforeEach
    public void setUp() {
        scheduler = new Scheduler();
        drone = new DroneSubsystem(scheduler, 1);
        event = new FireIncidentEvent("14:03:15", 1, "FIRE DETECTED", "High", new Zone(1, 0, 0, 100, 100), 15);
    }

    @Test
    public void testInitialState() {
        assertTrue(scheduler.getState() instanceof IdleState);
    }

//    @Test
//    void testIdleState_NoEvents() {
//        scheduler.setState(new IdleState());
//        scheduler.getState().handleEvents(scheduler);
//        assertTrue(scheduler.getState() instanceof IdleState);
//    }

    @Test
    void testIdleState_FireEventExists() {
        scheduler.receiveFireEvent(event);
        scheduler.receiveDroneEvent(drone);
        scheduler.setState(new IdleState());
        scheduler.getState().handleEvents(scheduler);
        assertTrue(scheduler.getState() instanceof ProcessingFireRequest);
    }

    @Test
    void testProcessingFireRequest_NoEvent() {
        scheduler.setState(new ProcessingFireRequest());
        scheduler.getState().handleEvents(scheduler);
        assertTrue(scheduler.getState() instanceof IdleState);
    }

    @Test
    void testProcessingFireRequest_ValidEvent() {
        scheduler.receiveFireEvent(event);
        scheduler.receiveDroneEvent(drone);
        scheduler.setState(new ProcessingFireRequest());
        scheduler.getState().handleEvents(scheduler);
        assertEquals(event, scheduler.getCurrentEvent());
        assertTrue(scheduler.getState() instanceof AssignDrones);
    }

    @Test
    void testAssignDrones_SufficientWaterAvailable() {
        scheduler.setCurrentEvent(event);
        event.setWaterNeeded(Scheduler.DRONE_CAPACITY);
        scheduler.receiveDroneEvent(drone);
        scheduler.setState(new AssignDrones());
        scheduler.getState().handleEvents(scheduler);
        assertTrue(scheduler.getState() instanceof IdleState);
    }

    @Test
    void testAssignDrones_DronesNotAvailable() {
        scheduler.setCurrentEvent(event);
        event.setWaterNeeded(Scheduler.DRONE_CAPACITY);
        scheduler.setState(new AssignDrones());
        scheduler.getState().handleEvents(scheduler);
        assertTrue(scheduler.getState() instanceof IdleState);
    }

    @Test
    void testAssignDrones_MultipleDronesNeeded() {
        scheduler.setCurrentEvent(event);
        event.setWaterNeeded(Scheduler.DRONE_CAPACITY + 1);
        scheduler.setState(new AssignDrones());
        scheduler.getState().handleEvents(scheduler);
        assertTrue(scheduler.getState() instanceof IdleState);
    }

    @Test
    void testReschedule_WithEvents() {
        scheduler.getEventQueue().add(event);
        scheduler.setState(new Reschedule());
        scheduler.getState().handleEvents(scheduler);
        assertTrue(scheduler.getState() instanceof AssignDrones);
    }

    @Test
    void testReschedule_NoEvents() {
        scheduler.setState(new Reschedule());
        scheduler.getState().handleEvents(scheduler);
        assertTrue(scheduler.getState() instanceof IdleState);
    }
}

 */
