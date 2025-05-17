import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.sql.Time;

import static java.lang.Math.ceil;
import static java.lang.Thread.getDefaultUncaughtExceptionHandler;
import static java.lang.Thread.sleep;

/**
 * The {@code DroneSubsystem} class represents a drone that responds to fire incidents.
 * It navigates to a specified fire zone, drops a firefighting agent, and then returns to base for refilling.
 * Implements {@code Runnable} to allow execution in a separate thread.
 */
public class DroneSubsystem {

    private DroneStateMachine stateMachine;

    private static final int TIME_SCALE = 10; // Scale time (for sleep calls)
    private static final double DRONE_SPEED = 2.8; // Speed of the drone
    private static final double NOZZLE_OPEN_TIME = 0.5; // Time in seconds for nozzle operation
    private static final double RATE_OF_WATER_DROP = 1.0; // Water release rate in L/s
    private static final int MAX_PAYLOAD = 15; // Maximum water payload (liters)

    private int droneID;
    private String status; // Status: "Idle", "En Route", "Arrived", "Dropping Agent", "Refilling", "Faulted"
    private int remainingPayload; // starts at 15
    private int waterNeeded;
    private Point currentCoordinates;
    private Point targetPoint;
    private String fault;
    private String faultType;

    private DatagramSocket socket;
    DatagramPacket sendPacket, receivePacket;
    private InetAddress schedulerAddr;
    private final int schedulerPort;

    /**
     * Constructs a {@code DroneSubsystem} instance
     *
     * @param host
     * @param schedulerPort
     */
    public DroneSubsystem(String host, int schedulerPort) {
        try {
            socket = new DatagramSocket();
            this.schedulerAddr = InetAddress.getByName(host);
        } catch (UnknownHostException | SocketException e) {
            throw new RuntimeException(e);
        }
        this.schedulerPort = schedulerPort;
        this.remainingPayload = MAX_PAYLOAD;
        this.currentCoordinates = new Point(0, 0);
        this.stateMachine = new DroneStateMachine();
    }

    /**
     * Calculates the travel time to a specified zone center.
     *
     * @param center The target zone center as a {@code Point}.
     * @return The estimated travel time in seconds.
     */
    public double calculateTravelTime(Point center) {
        System.out.println("Calculating travel time for " + center.x + ", " + center.y);
        System.out.println("Current coordinates: " + currentCoordinates);

        double distance = Math.sqrt(Math.pow(center.x - currentCoordinates.x, 2) + Math.pow(center.y - currentCoordinates.y, 2));

        return distance / DRONE_SPEED;
    }

    /**
     * Moves the drone to a specified target point and updates its coordinates.
     *
     * @param target The destination point.
     */
    public void travel(Point target) {
        double totalTravelTime = calculateTravelTime(target);
        // Update interval in seconds.. so updates coordinate location every 10 seconds
        final int updateInterval = 10;
        int fullIntervals = (int) (totalTravelTime / updateInterval);
        double leftoverTime = totalTravelTime - (fullIntervals * updateInterval);

        System.out.println("Drone travelling to: " + target);
        System.out.println("Calculated travel time: " + totalTravelTime + " seconds");

        // Use double precision for the current position for smooth updates.
        double currentX = currentCoordinates.x;
        double currentY = currentCoordinates.y;
        double targetX = target.x;
        double targetY = target.y;

        double totalDistance = Math.hypot(targetX - currentX, targetY - currentY);
        double dx = (targetX - currentX) / totalDistance;
        double dy = (targetY - currentY) / totalDistance;

        try {
            // Process full interval duration
            for (int i = 0; i < fullIntervals; i++) {
                sleep((updateInterval * 1000) / TIME_SCALE); // sleep for 10 seconds (scaled)
                currentX += DRONE_SPEED * updateInterval * dx;
                currentY += DRONE_SPEED * updateInterval * dy;
                System.out.println("Current coordinates: " + currentX + ", " + currentY);
                currentCoordinates = new Point((int) currentX, (int) currentY);
                //updateLocation();
            }
            // Process any remaining time
            if (leftoverTime > 0) {
                sleep((long)(leftoverTime * 1000 / TIME_SCALE));
                currentX += DRONE_SPEED * leftoverTime * dx;
                currentY += DRONE_SPEED * leftoverTime * dy;
                System.out.println("Current coordinates: " + currentX + ", " + currentY);
            }
            // Ensure the final position is exactly the target.
            currentX = targetX;
            currentY = targetY;
            System.out.println("TARGET REACHED!");
            currentCoordinates = new Point((int) currentX, (int) currentY);
            //updateLocation();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }



    /**
     * Calculates the time required to drop the specified amount of water.
     *
     * @param waterReleased The amount of water to be released (liters).
     * @return The estimated time in seconds.
     */
    public double calculateDropTime(int waterReleased) {
        return waterReleased / RATE_OF_WATER_DROP;
    }

    /**
     * Sets the drone's waterNeeded to the specified value
     *
     * @param waterNeeded The specified water needed
     */
    public void setWaterNeeded(int waterNeeded) {
        this.waterNeeded = waterNeeded;
    }

    /**
     * Gets the drone's waterNeeded
     *
     * @return The drone's waterNeeded value
     */
    public int getWaterNeeded() {
        return waterNeeded;
    }

    /**
     * Retrieves the drone's unique ID.
     *
     * @return The drone's ID.
     */
    public int getDroneID() {
        return droneID;
    }

    /**
     * Retrieves the current status of the drone.
     *
     * @return The drone's status as a string.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Retrieves the remaining payload of the drone.
     *
     * @return The remaining payload in liters.
     */
    public int getRemainingPayload() {
        return remainingPayload;
    }

    /**
     * Sets the drone's status.
     *
     * @param status The new status of the drone.
     */
    public void setStatus(String status) {
        this.status = status;
    }



    /**
     * Gets the drone's target point
     *
     * @return The drone's target point
     */
    public Point getTargetPoint(){
        return targetPoint;
    }

    /**
     * Sets the drone's target point to the specified value
     *
     * @param targetPoint The specified target point
     */
    public void setTargetPoint(Point targetPoint) {
        this.targetPoint = targetPoint;
    }

    /**
     * Gets the current fault associated with the drone.
     *
     * @return The fault string.
     */
    public String getFault() {
        return fault;
    }

    /**
     * Sets a fault condition for the drone.
     *
     * @param fault The fault string.
     */
    public void setFault(String fault) {
        this.fault = fault;
    }

    /**
     * Gets the fault type associated with the drone.
     *
     * @return The fault type string.
     */
    public String getFaultType() {
        return faultType;
    }

    /**
     * Sets the fault type for the drone.
     *
     * @param faultType The fault type string.
     */
    public void setFaultType(String faultType) {
        this.faultType = faultType;
    }

    /**
     * Simulates the opening of Drone Nozzle as per Drone specifications
     */
    public void openNozzle() {
        System.out.println("Drone Opens Nozzle to Release Water");
        try {
            sleep((int) (NOZZLE_OPEN_TIME * 1000) / TIME_SCALE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simulates the payload drop operation for firefighting.
     * It calculates the volume to be released and updates internal states accordingly.
     */
    public void dropPayload () {
        System.out.println("Drone calculating water to be released");
        int waterReleased = 0;
        //System.out.println("DEBUG: task: " + task.getTime() + " " + task.getZoneId() + " " + task.getEventType() + " " + task.getSeverity() + " " + task.getZone() + " " + task.getWaterNeeded());
        //System.out.println("DEBUG: water needed: " + task.getWaterNeeded() + " remaining payload: " + remainingPayload);
        if(waterNeeded < remainingPayload) {
            waterReleased = waterNeeded;
        } else if (waterNeeded >= remainingPayload) {
            waterReleased = remainingPayload;
        }
        try {
            System.out.println("Drone starts releasing water: " + waterReleased + " L");
            sleep((int) (this.calculateDropTime(waterReleased) * 1000) / TIME_SCALE);
            System.out.println("Drone stops releasing water");
            waterNeeded = waterNeeded - waterReleased;
            remainingPayload = remainingPayload - waterReleased;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Simulates closing the nozzle after water release.
     */
    public void closeNozzle() {
        System.out.println("Drone closes nozzle");
        try {
            sleep((int) (NOZZLE_OPEN_TIME * 1000) / TIME_SCALE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Refills the drone's payload to maximum capacity.
     */
    public void refillPayload() {
        System.out.println("Drone Refilling");
        remainingPayload = MAX_PAYLOAD;
        System.out.println("Drone Refilled, remaining payload: " + remainingPayload);
    }

    /**
     * Returns the drone status to the scheduler and resets to idle.
     */
    public void returnToScheduler() {
        System.out.println("Drone returns to Scheduler");

        String request = "RETURN_TO_SCHEDULER:" + this.droneID + ":" + this.waterNeeded; //"RETURN_TO_SCHEDULER:DRONEID:WATERNEEDED"

        String response = sendRequest(request); // response can be "RETURN_TO_SCHEDULER:OK" or "RETURNED_TO_SCHEDULER:FAILED"
        System.out.println("Drone returns to Scheduler: " + response);
    }

    /**
     * Waits for a task to be given to the drone by the scheduler
     */
    public void waitForTask() {
        String request = "GET_TASK:" + droneID;
        String response = sendRequest(request); // "TASKID:WATERNEEDED:X:Y:FAULT:FAULTTYPE"

        if(!response.equalsIgnoreCase("NO_TASK")) {
            String[] task = response.split(":");
            fault = task[4];
            faultType = task[5];
            waterNeeded = Integer.parseInt(task[1]);
            targetPoint = new Point(Integer.parseInt(task[2]), Integer.parseInt(task[3]));

            stateMachine.getCurrentState().droneAssigned(stateMachine, this);
        }
    }

    /**
     * Sends a request to scheduler to initialize new drone with a returned DroneID
     */
    public void initializeDrone(){
        String request = "INITIALIZE_DRONE";
        droneID = Integer.parseInt(sendRequest(request)); // "DRONEID"
    }

    /**
     * Notifies the scheduler that the drone has arrived at the target location.
     * Optionally introduces a corrupted message if simulating fault.
     *
     * @return Response from the scheduler.
     */
    public String notifyArrived() {
        String request = "NOTIFY_ARRIVED:" + droneID;
        if(fault.equals("CORRUPTED_MESSAGE")) {
            request = "NOFHIS_FNKSS:" + droneID;
        }
        return sendRequest(request); //"CORRUPTED_MESSAGE" or "OPEN_NOZZLE"
    }

    /**
     * Sends the drone's current location to the scheduler.
     *
     * @return Response from the scheduler.
     */
    public String updateLocation() {
        String request = "UPDATE_LOCATION:" + droneID + ":" + currentCoordinates.x + ":" + currentCoordinates.y; //UPDATE_LOCATION:DRONE_ID:X:Y
        return sendRequest(request);
    }

    /**
     * Sends a UDP request to the scheduler and waits for a response.
     *
     * @param request The message to send.
     * @return The response received from the scheduler.
     */
    public String sendRequest (String request) {
        try {
            //Convert request string into bytes
            byte[] outData = request.getBytes();

            //Build DatagramPacket to send to scheduler's address and port
            DatagramPacket sendPacket = new DatagramPacket(outData, outData.length, schedulerAddr, schedulerPort);

            //Send packet via our drone socket
            socket.send(sendPacket);
            System.out.println("Drone: Sent \"" + request + "\" to " + schedulerAddr);

            //Prepare a buffer to receive scheduler's response.
            byte[] inData = new byte[100];
            DatagramPacket receivePacket = new DatagramPacket(inData, inData.length);

            //created extra packet for debugging by scheduler team, commenting it as not needed now.
            /*
            byte[] buffer = new byte[1000];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            System.out.println("Response from Scheduler: "+ new String(packet.getData(), 0, packet.getLength()));
             */

            //Block until a packet is received from scheduler.
            socket.receive(receivePacket);



            //Convert response to string and return it
            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println("Drone: Received response \"" + response + "\" from " + schedulerAddr);
            return response;

        } catch(IOException e) {
            e.printStackTrace();
            return "ERROR: Drone I/O Exception";
        }

    }

    /**
     * Shuts down the drone subsystem.
     * Terminates the application.
     */
    public void shutdown(){
        System.out.println("Drone: Shutting down");
        System.exit(0);
    }

    /**
     * Main method to start the DroneSubsystem.
     * Connects to the scheduler and continuously waits for new tasks.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {

        int schedulerPort = 5000;
        String address = "localhost";

        DroneSubsystem drone = new DroneSubsystem(address, schedulerPort);

        drone.initializeDrone();
        while(true) {
            drone.waitForTask();
        }
    }
}



