/**
 * SchedulerState defines the operations that can be performed on the scheduler's state.
 * Each state must provide implementations for processing fire requests, assigning tasks,
 * waiting for drones, returning to idle, handling faults, and displaying the current state.
 */
interface SchedulerState {
    /**
     * Processes a fire request.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    void processFireRequest(SchedulerStateMachine context, Scheduler scheduler);

    /**
     * Assigns a task to available drones.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    void assignTask(SchedulerStateMachine context, Scheduler scheduler);

    /**
     * Waits for drones to become available or report back.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    void waitForDrones(SchedulerStateMachine context, Scheduler scheduler);

    /**
     * Transitions the scheduler to the idle state.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    void returnToIdle(SchedulerStateMachine context, Scheduler scheduler);

    /**
     * Handles faults that occur during scheduling.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    void handleFault(SchedulerStateMachine context, Scheduler scheduler);

    /**
     * Displays the current state.
     */
    void displayState();
}

/**
 * IdleState represents the scheduler state when it is idle and waiting for fire events.
 */
class IdleState implements SchedulerState {
    /**
     * Processes a fire request by transitioning to the ProcessingFireRequest state.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    @Override
    public void processFireRequest(SchedulerStateMachine context, Scheduler scheduler) {
        context.setState("ProcessingFireRequest");
    }

    /**
     * No task assignment is done in IdleState.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void assignTask(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * No waiting operation in IdleState.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void waitForDrones(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Already in IdleState, so no action is taken.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void returnToIdle(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * No fault handling is performed in IdleState.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void handleFault(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Displays the current state as Idle.
     */
    public void displayState() {
        System.out.println("[State]: Idle — Waiting for fire events.");
    }
}

/**
 * ProcessingFireRequest state represents the scheduler when it is processing a fire request.
 */
class ProcessingFireRequest implements SchedulerState {
    /**
     * In ProcessingFireRequest, processing an additional fire request is a no-op.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void processFireRequest(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Transitions to the AssigningTask state to assign tasks based on the fire request.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void assignTask(SchedulerStateMachine context, Scheduler scheduler) {
        context.setState("AssigningTask");
    }

    /**
     * No waiting operation is performed in ProcessingFireRequest.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void waitForDrones(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * No transition to idle occurs in ProcessingFireRequest.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void returnToIdle(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * No fault handling is performed in ProcessingFireRequest.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void handleFault(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Displays the current state as processing fire request.
     */
    public void displayState() {
        System.out.println("[State]: Processing fire request...");
    }
}

/**
 * AssigningTask state represents the scheduler when it is assigning drones to tasks.
 */
class AssigningTask implements SchedulerState {
    /**
     * In AssigningTask, processing a new fire request is not performed.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void processFireRequest(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * In AssigningTask, no additional task assignment is performed.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void assignTask(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Transitions to the WaitingForDrones state to wait for drone responses.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void waitForDrones(SchedulerStateMachine context, Scheduler scheduler) {
        context.setState("WaitingForDrones");
    }

    /**
     * No transition to idle occurs in AssigningTask.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void returnToIdle(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * No fault handling is performed in AssigningTask.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void handleFault(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Displays the current state as assigning drones to a task.
     */
    public void displayState() {
        System.out.println("[State]: Assigning drones to task.");
    }
}

/**
 * WaitingForDrones state represents the scheduler when it is waiting for drones to return or report.
 */
class WaitingForDrones implements SchedulerState {
    /**
     * In WaitingForDrones, processing a new fire request is not performed.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void processFireRequest(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * In WaitingForDrones, no new task assignment is performed.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void assignTask(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * No additional waiting is initiated in WaitingForDrones.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void waitForDrones(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Transitions the scheduler state back to Idle.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void returnToIdle(SchedulerStateMachine context, Scheduler scheduler) {
        context.setState("Idle");
    }

    /**
     * Handles faults by transitioning the state machine to HandlingFault.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void handleFault(SchedulerStateMachine context, Scheduler scheduler) {
        context.setState("HandlingFault");
    }

    /**
     * Displays the current state as waiting for drones.
     */
    public void displayState() {
        System.out.println("[State]: Waiting for drones to return or report.");
    }
}

/**
 * HandlingFault state represents the scheduler when it is handling a drone or system fault.
 */
class HandlingFault implements SchedulerState {
    /**
     * In HandlingFault, processing a new fire request is not performed.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void processFireRequest(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * In HandlingFault, no new task assignment is performed.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void assignTask(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * In HandlingFault, no waiting for drones is initiated.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void waitForDrones(SchedulerStateMachine context, Scheduler scheduler) {}

    /**
     * Transitions the scheduler state back to Idle.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void returnToIdle(SchedulerStateMachine context, Scheduler scheduler) {
        context.setState("Idle");
    }

    /**
     * In HandlingFault, additional fault handling is either a no-op or maintained.
     *
     * @param context   the state machine context
     * @param scheduler the scheduler instance
     */
    public void handleFault(SchedulerStateMachine context, Scheduler scheduler) {
        // Remain in HandlingFault state or transition as needed.
    }

    /**
     * Displays the current state as handling a fault.
     */
    public void displayState() {
        System.out.println("[State]: Handling drone/system fault.");
    }
}
