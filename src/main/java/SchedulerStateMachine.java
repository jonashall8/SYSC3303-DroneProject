import java.util.HashMap;
import java.util.Map;

/**
 * SchedulerStateMachine manages the state transitions for the scheduler.
 * It maintains a mapping of state names to SchedulerState objects and
 * allows transitions between these states.
 */
public class SchedulerStateMachine {

    /** The current state of the scheduler. */
    private SchedulerState currentState;
    
    /** A map of state names to SchedulerState instances. */
    private final Map<String, SchedulerState> states;

    /**
     * Constructs a new SchedulerStateMachine and initializes the available states.
     * The initial state is set to "Idle".
     */
    public SchedulerStateMachine() {
        states = new HashMap<>();
        states.put("Idle", new IdleState());
        states.put("ProcessingFireRequest", new ProcessingFireRequest());
        states.put("AssigningTask", new AssigningTask());
        states.put("WaitingForDrones", new WaitingForDrones());
        states.put("HandlingFault", new HandlingFault());

        currentState = states.get("Idle");
    }

    /**
     * Returns the current state of the scheduler.
     *
     * @return the current SchedulerState.
     */
    public SchedulerState getCurrentState() {
        return currentState;
    }

    /**
     * Transitions the state machine to the specified state.
     * If the state name exists in the state map, the current state is updated
     * and its displayState() method is called to show the new state.
     * Otherwise, an error message is printed.
     *
     * @param stateName the name of the state to transition to.
     */
    public void setState(String stateName) {
        SchedulerState nextState = states.get(stateName);
        if (nextState != null) {
            currentState = nextState;
            currentState.displayState();
        } else {
            System.err.println("[StateMachine]: Invalid state transition attempt to: " + stateName);
        }
    }

    /**
     * Resets the state machine to the Idle state.
     */
    public void resetToIdle() {
        setState("Idle");
    }
}
