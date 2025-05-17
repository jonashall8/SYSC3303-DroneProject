import java.util.LinkedList;
import java.util.Queue;

/**
 * FireEventList maintains a queue of fire incident events and provides
 * synchronized methods to add, remove, and check events. It is used to
 * coordinate the assignment of drones to fire incidents.
 */
public class FireEventList {
    /**
     * A reference to a fire incident event.
     * (Note: This field is not currently used in the logic.)
     */
    private FireIncidentEvent fireEvent;
    
    /**
     * Queue to hold fire incident events.
     */
    private Queue<FireIncidentEvent> list;
    
    /**
     * Event status flag.
     */
    private boolean eventStatus = false;
    
    /**
     * An event ID counter.
     */
    private int eventID = 0;

    /**
     * Constructs a new FireEventList with an empty event queue.
     */
    public FireEventList() {
        list = new LinkedList<>();
    }

    /**
     * Adds a fire incident event to the list if no event with the same zone ID is already present.
     * Notifies all waiting threads after adding the event.
     *
     * @param event the fire incident event to add
     */
    public synchronized void addEvent(FireIncidentEvent event) {
        // Note: The following assignment seems unintended. Consider if it should be:
        // this.fireEvent = event;
        this.fireEvent = fireEvent;

        // Check if an event with the same zone ID already exists.
        for (FireIncidentEvent fireEvent : list) {
            if (fireEvent.getZoneId() == event.getZoneId()) {
                notifyAll();
                return;
            }
        }
        list.add(event);
        notifyAll();
    }

    /**
     * Checks whether there are any fire incident events in the list.
     *
     * @return true if the list is not empty; false otherwise
     */
    public synchronized boolean checkEvents() {
        boolean status = false;
        if (!list.isEmpty()) {
            status = true;
        }
        return status;
    }

    /**
     * Removes and returns the next fire incident event from the list.
     *
     * @return the next fire incident event, or null if the list is empty
     */
    public synchronized FireIncidentEvent removeEvent() {
        FireIncidentEvent fireEvent = list.poll();
        return fireEvent;
    }

    /**
     * Checks whether the event list is empty.
     *
     * @return true if the list is empty; false otherwise
     */
    public synchronized boolean isListEmpty() {
        boolean isEmpty = false;
        if (list.isEmpty()) {
            isEmpty = true;
        }
        return isEmpty;
    }

    /**
     * Blocks until the event list is not empty.
     * This method can be used to stop drone assignment until an event is available.
     */
    public synchronized void stopAssigningDrones() {
        while (list.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Optionally handle the interruption.
            }
        }
    }

    /**
     * Returns the underlying queue of fire incident events.
     *
     * @return the event queue
     */
    public synchronized Queue<FireIncidentEvent> getList() {
        return list;
    }

    /**
     * Returns the number of fire incident events currently in the list.
     *
     * @return the size of the event list
     */
    public synchronized int getListSize() {
        return list.size();
    }

    /**
     * Blocks until the event list is no longer empty.
     */
    public synchronized void handleEmptyList() {
        while (isListEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Optionally handle the interruption.
            }
        }
    }

    /**
     * Notifies all waiting threads, e.g., to alert the scheduler that an event is available.
     */
    public synchronized void notifyScheduler() {
        notifyAll();
        System.out.println("notified scheduler to wait");
    }
}
