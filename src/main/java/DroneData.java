import java.awt.*;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents data for an individual drone in the system.
 * This class stores the drone's ID, location, payload capacity, water capacity,
 * and various status flags (e.g., whether it has requested a task, is lost, or has completed its job).
 */
public class DroneData {
    /**
     * Unique identifier for the drone.
     */
    private int droneID;
    
    /**
     * The target location (in grid coordinates) the drone is assigned to.
     */
    private Point targetLocation;
    
    /**
     * The current location (in grid coordinates) of the drone.
     */
    private Point location;
    
    /**
     * The remaining payload of the drone (e.g., water remaining for extinguishing fire).
     */
    private int remainingPayload;
    
    /**
     * The maximum water capacity of the drone.
     */
    private int maxWater;
    
    /**
     * The current available water of the drone.
     */
    private int availableWater;
    
    /**
     * Flag indicating whether the drone has requested a task.
     */
    boolean hasRequested;
    
    /**
     * The event ID associated with the drone's current assignment.
     */
    int eventID;
    
    /**
     * The network address of the drone.
     */
    InetAddress address;
    
    /**
     * Map representing event status flags (keyed by event ID).
     */
    private Map<Integer, Boolean> eventStatus;
    
    /**
     * Flag indicating whether the drone is lost.
     */
    boolean isLost = false;
    
    /**
     * The travel time (in seconds or other unit) for the drone.
     */
    int travelTime = 0;
    
    /**
     * Flag indicating whether the drone has arrived at its destination.
     */
    boolean hasArrived = false;
    
    /**
     * Flag indicating whether the drone has completed its current job.
     */
    boolean completedJob = false;

    /**
     * The drone's job status.
     */
    int droneJobStatus;
    
    /**
     * Indicates if the drone is in the process of coming back (1 for coming back, -1 for not coming back, 0 if not set).
     */
    int isComingBack = 0;
    
    /**
     * Flag indicating if the drone is currently at the base.
     */
    boolean isAtBase;

    /**
     * The port number associated with the drone's communication.
     */
    int port;

    /**
     * Constructs a new DroneData instance.
     *
     * @param droneID          the unique ID of the drone
     * @param location         the initial location of the drone (in grid coordinates)
     * @param remainingPayload the initial remaining payload (e.g., water) of the drone
     * @param address          the InetAddress of the drone
     * @param port             the port number for communication with the drone
     */
    public DroneData(int droneID, Point location, int remainingPayload, InetAddress address, int port) {
        this.droneID = droneID;
        this.targetLocation = null;
        this.location = location;
        this.remainingPayload = remainingPayload;
        hasRequested = false;  // Initially not requested for a task.
        this.address = address;
        this.port = port;
        eventStatus = new HashMap<>();
        isLost = false;
        droneJobStatus = 10;
        this.maxWater = 15;  // Maximum water capacity (adjustable)
        this.availableWater = this.maxWater;
        this.targetLocation = null;
        isAtBase = true;
    }

    /**
     * Initializes a new DroneData instance.
     * (Method stub: implementation can be added as needed.)
     *
     * @return null (currently not implemented)
     */
    public static DroneData initializeDroneData() {
        return null;
    }

    /**
     * Returns the drone's unique identifier.
     *
     * @return the droneID
     */
    public int getDroneID() {
        return droneID;
    }

    /**
     * Returns the current location of the drone.
     *
     * @return the current location as a Point
     */
    public Point getLocation() {
        return location;
    }

    /**
     * Returns the current available water of the drone.
     *
     * @return the available water
     */
    public int getAvailableWater() {
        return availableWater;
    }

    /**
     * Refills the drone's water back to its maximum capacity.
     */
    public void refillWater() {
        this.availableWater = this.maxWater;
    }

    /**
     * Sets the target location for the drone.
     *
     * @param targetLocation the target location (in grid coordinates)
     */
    public void setTargetLocation(Point targetLocation) {
        this.targetLocation = targetLocation;
    }

    /**
     * Returns the remaining payload of the drone.
     *
     * @return the remaining payload
     */
    public int getRemainingPayload() {
        return remainingPayload;
    }

    /**
     * Sets the drone's unique identifier.
     *
     * @param droneID the new drone ID
     */
    public void setDroneID(int droneID) {
        this.droneID = droneID;
    }

    /**
     * Sets the current location of the drone.
     *
     * @param location the new location (in grid coordinates)
     */
    public void setLocation(Point location) {
        this.location = location;
    }

    /**
     * Sets the drone's remaining payload.
     *
     * @param remainingPayload the new remaining payload
     */
    public void setRemainingPayload(int remainingPayload) {
        this.remainingPayload = remainingPayload;
    }

    /**
     * Returns whether the drone is at the base.
     *
     * @return true if the drone is at base; false otherwise
     */
    public boolean isAtBase() {
        return isAtBase;
    }

    /**
     * Sets whether the drone is at the base.
     *
     * @param atBase true if the drone is at base; false otherwise
     */
    public void setAtBase(boolean atBase) {
        this.isAtBase = atBase;
    }

    /**
     * Returns the value indicating whether the drone is coming back.
     *
     * @return the isComingBack value (1, -1, or 0)
     */
    public int getIsComingBack() {
        return isComingBack;
    }

    /**
     * Sets the value indicating whether the drone is coming back.
     *
     * @param isComingBack the new isComingBack value (1, -1, or 0)
     */
    public void setIsComingBack(int isComingBack) {
        this.isComingBack = isComingBack;
    }

    /**
     * Returns whether the drone has arrived at its destination.
     *
     * @return true if the drone has arrived; false otherwise
     */
    public boolean isHasArrived() {
        return hasArrived;
    }

    /**
     * Sets the flag indicating whether the drone has arrived.
     * If set to true, the drone is marked as not lost.
     *
     * @param hasArrived true if the drone has arrived; false otherwise
     */
    public void setHasArrived(boolean hasArrived) {
        this.hasArrived = hasArrived;
        if (hasArrived) {
            isLost = false;
        }
    }

    /**
     * Returns whether the drone has completed its job.
     *
     * @return true if the job is completed; false otherwise
     */
    public boolean isCompletedJob() {
        return completedJob;
    }

    /**
     * Sets the completed job status for the drone.
     * If the job is not completed, the drone is marked as lost.
     *
     * @param completedJob true if the job is completed; false otherwise
     */
    public void setCompletedJob(boolean completedJob) {
        this.completedJob = completedJob;
        System.out.println("[" + Thread.currentThread().getName() + "] -> Drone Data: drone#" + droneID + " Has COMPLETED_JOB? " + completedJob);
        if (!completedJob) {
            setLost(true);
        }
    }

    /**
     * Returns the travel time for the drone.
     *
     * @return the travel time
     */
    public int getTravelTime() {
        return travelTime;
    }

    /**
     * Sets the travel time for the drone.
     *
     * @param travelTime the travel time to set
     */
    public void setTravelTime(int travelTime) {
        this.travelTime = travelTime;
    }

    /**
     * Returns the drone's job status.
     *
     * @return the job status as an integer
     */
    public int getDroneJobStatus() {
        return droneJobStatus;
    }

    /**
     * Sets the drone's job status.
     *
     * @param droneJobStatus the job status to set
     */
    public void setDroneJobStatus(int droneJobStatus) {
        this.droneJobStatus = droneJobStatus;
    }

    /**
     * Returns whether the drone is lost.
     *
     * @return true if the drone is lost; false otherwise
     */
    public boolean isLost() {
        return isLost;
    }

    /**
     * Sets the lost status for the drone. If set to true, the drone is marked as not requested.
     *
     * @param lost true if the drone is lost; false otherwise
     */
    public void setLost(boolean lost) {
        isLost = lost;
        if (lost) {
            setHasRequested(false);
        }
        System.out.println("[" + Thread.currentThread().getName() + "] -> Drone Data: drone#" + droneID + " is lost");
    }

    /**
     * Returns the network address of the drone.
     *
     * @return the InetAddress of the drone
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Returns the port number associated with the drone.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the event ID associated with the drone.
     *
     * @return the event ID
     */
    public int getEventID() {
        return eventID;
    }

    /**
     * Sets the event ID for the drone.
     *
     * @param eventID the event ID to set
     */
    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    /**
     * Returns whether the drone has requested a task.
     *
     * @return true if the drone has requested; false otherwise
     */
    public boolean getHasRequested() {
        return hasRequested;
    }

    /**
     * Sets whether the drone has requested a task.
     *
     * @param isBusy true if the drone has requested a task; false otherwise
     */
    public void setHasRequested(boolean isBusy) {
        this.hasRequested = isBusy;
    }

    /**
     * Returns a string representation of the drone data.
     *
     * @return a string describing the drone's ID, location, and remaining payload
     */
    @Override
    public String toString() {
        return "DroneData{" +
                "droneID=" + droneID +
                ", location=" + location +
                ", remainingPayload=" + remainingPayload +
                '}';
    }
}
