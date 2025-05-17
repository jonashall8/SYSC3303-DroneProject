import java.awt.*;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * The DroneFleet class manages a collection of drones and provides methods
 * for adding, removing, and tracking drones as well as updating their locations.
 * It also interacts with a FireEventList to coordinate events and uses a TimeStamp
 * inner class for scheduling periodic drone check-ups.
 */
public class DroneFleet {
    /**
     * A temporary DroneData reference (not used for fleet storage).
     */
    DroneData drone;
    
    /**
     * An integer ID (not actively used).
     */
    int ID;
    
    /**
     * A thread-safe map holding the fleet of drones.
     * Key: Drone ID, Value: DroneData object.
     */
    private Map<Integer, DroneData> droneFleet;
    
    /**
     * A reference to the FireEventList.
     */
    private FireEventList eventList;
    
    /**
     * Flag indicating whether a drone has been lost.
     */
    boolean isLost = false;
    
    /**
     * Timer instance used for scheduling drone check-ups.
     */
    private TimeStamp timer;

    /**
     * Constructs a new DroneFleet with an empty fleet and initializes
     * the timer for scheduling tasks.
     *
     * @param eventList the shared FireEventList instance
     */
    public DroneFleet(FireEventList eventList) {
        droneFleet = new ConcurrentHashMap<>();
        this.eventList = eventList;
        timer = new TimeStamp();
    }

    /**
     * Adds a new drone to the fleet.
     *
     * @param assignedId the ID to assign to the drone
     * @param drone      the DroneData object to add
     */
    public synchronized void addDrone(int assignedId, DroneData drone) {
        droneFleet.put(assignedId, drone);
        notifyAll();
    }

    /**
     * Notifies waiting threads that a drone has returned to base.
     */
    public synchronized void droneReturnedToBase() {
        notifyAll();
    }

    /**
     * Removes a drone from the fleet.
     *
     * @param assignedId the ID of the drone to remove
     */
    public synchronized void removeDrone(int assignedId) {
        droneFleet.remove(assignedId);
        System.out.println("[" + Thread.currentThread().getName() + "] -> DroneData: drone#" + assignedId + " has been removed from the fleet");
    }

    /**
     * Marks the drone with the specified ID as unavailable.
     *
     * @param ID the ID of the drone to mark as unavailable
     */
    public synchronized void isUnavailable(int ID) {
        droneFleet.get(ID).setHasRequested(false);
    }

    /**
     * Marks the drone with the specified ID as available.
     *
     * @param ID the ID of the drone to mark as available
     */
    public synchronized void isAvailable(int ID) {
        DroneData drone = droneFleet.get(ID);
        drone.setHasRequested(true);
        notifyAll();
    }

    /**
     * Checks if the fleet is empty.
     *
     * @return true if the fleet is empty; false otherwise
     */
    public synchronized boolean isFleetEmpty() {
        return droneFleet.isEmpty();
    }

    /**
     * Counts the number of available drones.
     * A drone is considered available if it has requested and is at base.
     *
     * @return the count of available drones
     */
    public synchronized int getAvailableDrones() {
        int count = 0;
        for (DroneData drone : droneFleet.values()) {
            if (drone.getHasRequested() && drone.isAtBase()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns a temporary DroneData reference.
     *
     * @return a DroneData object (field 'drone')
     */
    public synchronized DroneData getDrone() {
        return drone;
    }

    /**
     * Returns a collection of all drones in the fleet.
     *
     * @return a Collection of DroneData objects
     */
    public synchronized Collection<DroneData> getDrones() {
        return droneFleet.values();
    }

    /**
     * Returns the drone with the specified ID.
     *
     * @param ID the drone ID
     * @return the DroneData object with that ID
     */
    public synchronized DroneData getADrone(int ID) {
        return droneFleet.get(ID);
    }

    /**
     * Notifies waiting threads (used to wake up the scheduler).
     */
    public synchronized void notifyScheduler() {
        notifyAll();
    }

    /**
     * Returns the entire drone fleet map.
     *
     * @return a Map containing all drones in the fleet
     */
    public synchronized Map<Integer, DroneData> getWholeFleet() {
        return droneFleet;
    }

    /**
     * Checks for fire events in the event list and notifies waiting threads if events exist.
     */
    public synchronized void checkEvents() {
        if (eventList.checkEvents()) {
            notifyAll();
        }
    }

    /**
     * Blocks until at least one drone is available.
     */
    public synchronized void stopAssigningDrones() {
        while (getAvailableDrones() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // Optionally handle interruption.
            }
        }
    }

    /**
     * Schedules a check-up for a drone after a specified delay.
     *
     * @param delay the delay in seconds
     * @param drone the DroneData object to check
     */
    public synchronized void trackDrones(int delay, DroneData drone) {
        timer.scheduleDroneCheckUp(delay, drone);
    }

    /**
     * Schedules a nozzle check-up for a drone after a specified delay.
     *
     * @param delay the delay in seconds
     * @param drone the DroneData object to check for nozzle issues
     */
    public synchronized void trackDroneNozzle(int delay, DroneData drone) {
        timer.scheduleDroneNozzleCheckUp(delay, drone);
    }

    /**
     * Updates the stored location of the drone with the specified ID.
     *
     * @param droneID  the ID of the drone to update
     * @param location the new location as a Point
     */
    public void updateLocation(int droneID, Point location) {
        droneFleet.get(droneID).setLocation(location);
    }

    /**
     * Checks if any drones in the fleet are available at base.
     *
     * @return true if at least one drone is at base; false otherwise
     */
    public synchronized boolean areDronesAvailabaleAtBase() {
        for (DroneData drone : droneFleet.values()) {
            if (drone.isAtBase()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Blocks until at least one drone returns to base.
     */
    public synchronized void waitForDronesToComeBack() {
        while (!areDronesAvailabaleAtBase()) {
            try {
                System.out.println("[" + Thread.currentThread().getName() + "]: Waiting for at least one drone to come back to base. Until in wait...");
                wait();
            } catch (InterruptedException e) {
                // Optionally handle interruption.
            }
        }
    }

    /**
     * Inner class TimeStamp is used to schedule periodic check-ups for drones.
     */
    class TimeStamp {
        /**
         * Flag indicating if a drone is lost (not used directly).
         */
        private boolean lost = false;

        /**
         * A ScheduledExecutorService for scheduling delayed tasks.
         */
        private final ScheduledExecutorService delayedScheduler = Executors.newScheduledThreadPool(1);

        /**
         * Schedules a nozzle check-up for a drone.
         * After the specified delay, the drone is checked to determine if its nozzle is jammed.
         *
         * @param delay the delay in seconds before the check-up
         * @param drone the DroneData object to check
         */
        public void scheduleDroneNozzleCheckUp(int delay, DroneData drone) {
            int droneID = drone.getDroneID();
            DroneData droneInProgress = getADrone(droneID);
            final Runnable beeper = new Runnable() {
                @Override
                public synchronized void run() {
                    markDroneBroken(droneInProgress);
                }
            };
            delayedScheduler.schedule(beeper, delay, SECONDS);
        }

        /**
         * Schedules a check-up to determine if a drone is lost.
         * After the specified delay, the drone is checked to see if it has arrived.
         *
         * @param delay the delay in seconds before the check-up
         * @param drone the DroneData object to check
         */
        public void scheduleDroneCheckUp(int delay, DroneData drone) {
            int droneID = drone.getDroneID();
            DroneData droneInProgress = getADrone(droneID);
            final Runnable beeper = new Runnable() {
                @Override
                public synchronized void run() {
                    markDroneLost(droneInProgress);
                }
            };
            delayedScheduler.schedule(beeper, delay, SECONDS);
        }

        /**
         * Marks a drone as broken if it has not returned properly.
         * If the drone is coming back, it is confirmed and marked as at base.
         * Otherwise, it is marked as lost.
         *
         * @param drone the DroneData object to mark
         */
        public synchronized void markDroneBroken(DroneData drone) {
            if (drone.getIsComingBack() == 1) {
                isLost = false;
                drone.setIsComingBack(1);
                drone.setAtBase(true);
                droneReturnedToBase();
                System.out.println("[" + Thread.currentThread().getName() + "] --> DroneData: Drone#" + drone.getDroneID() + " sent confirmation signal of coming back");
            } else {
                isLost = true;
                drone.setIsComingBack(-1);
                System.out.println("[" + Thread.currentThread().getName() + "] --> DroneData: Drone#" + drone.getDroneID() + " is lost due to nozzle jam!");
            }
        }

        /**
         * Marks a drone as lost if it has not arrived at its destination.
         * Otherwise, it confirms arrival and updates the drone's job status.
         *
         * @param drone the DroneData object to mark
         */
        public synchronized void markDroneLost(DroneData drone) {
            if (!drone.isHasArrived()) {
                isLost = true;
                drone.setDroneJobStatus(-1);
                System.out.println("[" + Thread.currentThread().getName() + "] --> DroneData: Drone#" + drone.getDroneID() + " is lost!");
            } else {
                isLost = false;
                System.out.println("[" + Thread.currentThread().getName() + "] --> DroneData: Drone#" + drone.getDroneID() + " sent confirmation signal of arrival");
                drone.setDroneJobStatus(1);
            }
            System.out.println("[" + Thread.currentThread().getName() + "]: timer should be shut down now");
        }

        /**
         * Returns whether a drone is marked as lost.
         *
         * @return true if the drone is lost; false otherwise
         */
        public boolean isDroneLost() {
            return lost;
        }
    }
}
