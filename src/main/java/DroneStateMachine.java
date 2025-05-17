
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import static java.lang.Thread.sleep;


/**
 * The {@code DroneState} interface represents state classes of droneSubsystem that corresponds to state actions
 * and changes. It initializes the barebone methods of all the state actions and events for the inherited
 * classes to implement
 */
interface DroneState {

    /**
     * Handles the event for when the Drone is assigned.
     *
     * @param context The content of the state machine
     * @param drone   The drone subsystem instance.
     */
    void droneAssigned(DroneStateMachine context, DroneSubsystem drone);

    /**
     * Handles the event for when the Drone is arrived at the Zone.
     *
     * @param context The content of the state machine
     * @param drone   The drone subsystem instance.
     */
    void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone);

    /**
     * Handles the event when the drone opens its nozzle to release fire retardant.
     *
     * @param context The state machine context.
     * @param drone   The drone subsystem instance.
     */
    void openNozzle(DroneStateMachine context, DroneSubsystem drone);

    /**
     * Handles the event when the drone has finished dropping the agent.
     *
     * @param context The state machine context.
     * @param drone   The drone subsystem instance.
     */
    void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone);

    /**
     * Handles the event when the drone arrives back at the base.
     *
     * @param context The state machine context.
     * @param drone   The drone subsystem instance.
     */
    void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone);

    /**
     * Handles the event when the drone's payload is refilled.
     *
     * @param context The state machine context.
     * @param drone   The drone subsystem instance.
     */
    void payloadRefilled(DroneStateMachine context, DroneSubsystem drone);

    /**
     * Handles the event when the drone simulates a fault
     *
     * @param context The state machine context
     * @param drone   The drone subsystem instance
     */
    void faultHandled(DroneStateMachine context, DroneSubsystem drone);

    /**
     * Displays the current state of the drone.
     */
    void displayState();
}


/**
 * State class for representing when Drone is in Idle state, and ready to take on a task
 */
class Idle implements DroneState {
    @Override
    public void droneAssigned(DroneStateMachine context, DroneSubsystem drone) {
        context.setState("EnRoute");
        //drone.travel(drone.getTask().getZone().getCenter()); // calculate travel time and travel

        drone.travel(drone.getTargetPoint());
        context.getCurrentState().arrivedAtZone(context, drone); // state transition. Arrived at zone
    }

    @Override
    public void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void openNozzle(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void payloadRefilled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void faultHandled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void displayState(){
        System.out.println("Idle");
    }

}

/**
 * State class for representing when Drone is in en route to zone
 */

class EnRoute implements DroneState {

    @Override
    public void droneAssigned(DroneStateMachine context, DroneSubsystem drone) {}
    @Override
    public void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone) {
        if(drone.getFault().equals("DRONE_STUCK")) {
            context.setState("Faulted");
            context.getCurrentState().faultHandled(context, drone);
        } else {
            context.setState("Arrived");
            String response = drone.notifyArrived();
            if(response.equals("CORRUPTED_MESSAGED")) {
                context.setState("Faulted");
                context.getCurrentState().faultHandled(context, drone);
            } else if (response.equals("OPEN_NOZZLE")) {
                context.getCurrentState().openNozzle(context, drone); // arrived now open nozzle and drop agent, state transition
            }
        }
    }
    @Override
    public void openNozzle(DroneStateMachine context, DroneSubsystem drone) {}
    @Override
    public void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone) {}
    @Override
    public void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone) {}
    @Override
    public void payloadRefilled(DroneStateMachine context, DroneSubsystem drone) {}
    @Override
    public void faultHandled(DroneStateMachine context, DroneSubsystem drone) {}
    @Override
    public void displayState(){
        System.out.println("EnRoute");
    }
}

/**
 * State class for representing when drone has arrived at zone and is ready to start dropping agent
 */

class ArrivedAtZone implements DroneState {

    @Override
    public void droneAssigned(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void openNozzle(DroneStateMachine context, DroneSubsystem drone) {
        if(drone.getFault().equals("NOZZLE_JAM")) {
            System.out.println("Drone: Nozzle Jammed!");
            context.setState("Faulted");
            context.getCurrentState().faultHandled(context, drone);
        }
        else {
            drone.openNozzle(); // "DroppingAgent" state entry action
            context.setState("DroppingAgent");
            drone.dropPayload(); // "DroppingAgent" do activity
            drone.closeNozzle(); // "DroppingAgent" exit action
            context.getCurrentState().finishedDroppingAgent(context, drone); // State Transition
        }
    }

    @Override
    public void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void payloadRefilled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void faultHandled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void displayState(){
        System.out.println("Arrived at zone");
    }
}

/**
 * State class for representing when drone is dropping agent at zone
 */

class DroppingAgent implements DroneState {

    @Override
    public void droneAssigned(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void openNozzle(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone) {
        //if remaining payload < 5L
        if (drone.getRemainingPayload() < 5){
            context.setState("ReturningToBase");
            drone.returnToScheduler();
            drone.travel(new Point(0,0));
            context.getCurrentState().arrivedAtBase(context, drone);
        } else {
            drone.returnToScheduler();
            context.setState("Idle");
            drone.waitForTask();
            System.out.println("Back to state machine");
            context.getCurrentState().droneAssigned(context, drone);
            /**
             * Change made by scheduler team
             */
        }
    }
    @Override
    public void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void payloadRefilled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void faultHandled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void displayState(){
        System.out.println("Dropping agent at zone");
    }
}

/**
 * State class for representing when drone is returning to base
 */

class ReturningToBase implements DroneState {
    @Override
    public void droneAssigned(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void openNozzle(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone) {
        context.setState("Refilling");
        drone.refillPayload();
        context.getCurrentState().payloadRefilled(context, drone);
    }

    @Override
    public void payloadRefilled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void faultHandled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void displayState(){
        System.out.println("Returning to base");
    }
}

/**
 * State class for representing when drone is refilling agent at base.
 */

class Refilling implements DroneState {
    @Override
    public void droneAssigned(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void openNozzle(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void payloadRefilled(DroneStateMachine context, DroneSubsystem drone) {
        context.setState("Idle");
        System.out.println("Drone in Idle");

        drone.waitForTask();
        context.getCurrentState().droneAssigned(context, drone);
    }

    @Override
    public void faultHandled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void displayState(){
        System.out.println("Refilling agent at base");
    }
}

/**
 * State class for representing when drone is faulted
 */

class Faulted implements DroneState {
    @Override
    public void droneAssigned(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtZone(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void openNozzle(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void finishedDroppingAgent(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void arrivedAtBase(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void payloadRefilled(DroneStateMachine context, DroneSubsystem drone) {}

    @Override
    public void faultHandled(DroneStateMachine context, DroneSubsystem drone) {
        if (drone.getFaultType().equals("HARD_FAULT")) {
            System.out.println("Drone: Hard Fault: " + drone.getFault());
            drone.shutdown();
        } else {
            System.out.println("Drone: Transient Fault " + drone.getFault());
            drone.setFault("NO_FAULT");
            drone.setFaultType("N/A");
            context.setState("EnRoute");
            context.getCurrentState().arrivedAtZone(context, drone);
        }
    };

    @Override
    public void displayState(){
        System.out.println("Drone Faulted");
    }

}


/**
 * The {@code DroneStateMachine} class represents a drone that manages and holds the droneSubsystem's states
 *  as well as it's currentState. This class seeks to provide relevant methods for state manipulation (adding,
 *  setting and getting states)
 */
public class DroneStateMachine {
    private DroneState currentState;
    private Map<String, DroneState> states;

    /**
     * Constructs a new state machine for the crossing and initializes states with default values.
     */
    public DroneStateMachine() {
        states = new HashMap<>();
        // Add states to the map
        addState("Idle", new Idle());
        addState("EnRoute", new EnRoute());
        addState("Arrived", new ArrivedAtZone());
        addState("DroppingAgent", new DroppingAgent());
        addState("ReturningToBase", new ReturningToBase());
        addState("Refilling", new Refilling());
        addState("Faulted", new Faulted());

        // Set the initial state
        setState("Idle");

    }

    /**
     * Adds a state to the state machine.
     *
     * @param stateName The name of the state.
     * @param state     The state to be added.
     */
    public void addState(String stateName, DroneState state) {
        states.put(stateName, state);
    }

    /**
     * Sets the current state of the state machine.
     *
     * @param stateName The name of the state to set.
     */
    public void setState(String stateName) {
        this.currentState = getState(stateName);
    }

    /**
     * Retrieves a state from the state machine.
     *
     * @param stateName The name of the state to be retrieved.
     * @return The state corresponding to the given name.
     */
    public DroneState getState(String stateName) {
        return states.get(stateName);
    }

    /**
     * Retrieves the current state of the drone.
     *
     * @return The current state instance.
     */
    public DroneState getCurrentState() {
        return currentState;
    }


//    public DroneSubsystem getDrone() {
//        return drone;
//    }

}
