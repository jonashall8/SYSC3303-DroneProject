import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.List;

/**
 * The Scheduler class handles communication between the drone system and the fire incident system.
 * It manages incoming messages, assigns tasks to drones, tracks fire events, and handles faults.
 * The Scheduler implements Runnable so that it can run as a separate thread.
 */
public class Scheduler implements Runnable {

    /** Port used for communication with the drone system. */
    private static final int PORT_NUMBER_DRONE_SYSTEM = 5000;
    /** Port used for communication with the fire incident system. */
    private static final int PORT_NUMBER_FIRE_INCIDENT_SYSTEM = 6000;
    /** Maximum payload capacity (e.g., water capacity) of a drone. */
    private static final int MAX_PAYLOAD = 15;
    /** Drone travel speed (used for calculating travel time). */
    private static final double DRONE_SPEED = 2.8;
    /** Metrics log file name. */
    private static final String METRICS_LOG_FILE = "metrics_log.txt";

    /** Shared DroneFleet instance containing all drones. */
    DroneFleet drones;
    /** Queue of fire incident events. */
    private Queue<FireIncidentEvent> fireEvents;
    /** List of drone IDs that are currently available. */
    private List<Integer> availableDrones = new ArrayList<>();
    /** DatagramSocket for sending and receiving UDP messages. */
    private DatagramSocket socket;
    /** Current state of the scheduler (used in the state machine). */
    private SchedulerState state;
    /** Static counter for assigning unique drone IDs. */
    private static int nextDroneID = 1;
    /** Static counter for assigning unique event IDs. */
    private static int nextEventID = 1;
    /** Number of fire events processed. */
    private int numbFireEvents = 0;
    /** Flag indicating if there is a pending drone request. */
    private boolean pendingReq = false;
    /** State machine managing scheduler state transitions. */
    private SchedulerStateMachine stateMachine;
    /** Port on which the Scheduler listens for messages. */
    int port;
    /** List of fire events. */
    FireEventList eventList;
    /** Buffer for incoming drone requests. */
    DroneRequestBuffer bufferReq;
    /** In-progress fire events mapping (e.g., drone ID to event). */
    private InProgressEvents inProgressEvents;
    /** Metrics logger for logging dispatch and extinguish events. */
    private static MetricsLogger metricsLogger;
    /** Flag to indicate if stuck drones should be handled. */
    private static boolean shouldHandleStuckDrones = false;

    /**
     * Sets whether the scheduler should handle stuck drones.
     *
     * @param shouldHandleStuckDrones true if stuck drones should be handled; false otherwise
     */
    public static void setShouldHandleStuckDrones(boolean shouldHandleStuckDrones) {
        Scheduler.shouldHandleStuckDrones = shouldHandleStuckDrones;
    }

    /**
     * Constructs a new Scheduler.
     *
     * @param drones            the shared DroneFleet instance
     * @param bufferReq         the buffer containing incoming drone requests
     * @param eventList         the list of fire incident events
     * @param inProgressEvents  the in-progress events tracker
     * @param port              the port number on which the Scheduler will listen
     */
    public Scheduler(DroneFleet drones, DroneRequestBuffer bufferReq, FireEventList eventList, InProgressEvents inProgressEvents, int port) {
        this.port = port;
        this.drones = drones;
        this.eventList = eventList;
        state = new IdleState();
        pendingReq = false;
        this.bufferReq = bufferReq;
        this.inProgressEvents = inProgressEvents;
        try {
            this.metricsLogger = new MetricsLogger(METRICS_LOG_FILE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        this.stateMachine = new SchedulerStateMachine();
    }

    /**
     * Returns the current state of the scheduler.
     *
     * @return the current SchedulerState
     */
    public SchedulerState getState() {
        return state;
    }

    /**
     * Returns a map of all drones in the fleet.
     *
     * @return a Map where the key is the drone ID and the value is the corresponding DroneData
     */
    public Map<Integer, DroneData> getDrones() {
        return drones.getWholeFleet();
    }

    /**
     * Returns the queue of fire incident events.
     *
     * @return the Queue of FireIncidentEvent objects
     */
    public Queue<FireIncidentEvent> getFireEvents() {
        return fireEvents;
    }

    /**
     * Stops the scheduler by closing its DatagramSocket.
     * This is typically used during test teardown.
     */
    public void stop() {
        if (socket != null && !socket.isClosed()) {
            socket.close();  // Close socket to free the port
        }
    }

    /**
     * Returns the next event ID and increments the internal counter.
     *
     * @return the next event ID as an integer
     */
    private static int getNextEventID() {
        return nextEventID++;
    }

    /**
     * Calculates the travel time from a fixed starting point (assumed to be (0,0))
     * to the given center point, based on DRONE_SPEED.
     *
     * @param center the target center point
     * @return the calculated travel time as an integer
     */
    public int calculateTravelTime(Point center) {
        Point currentCoordinates = new Point(0, 0);
        System.out.println("Calculating travel time for " + center.x + ", " + center.y);
        System.out.println("Current coordinates: " + currentCoordinates);
        double distance = Math.sqrt(Math.pow(center.x - currentCoordinates.x, 2) + Math.pow(center.y - currentCoordinates.y, 2));
        return (int) (distance / DRONE_SPEED);
    }

    /**
     * Assigns tasks to drones based on available fire events.
     * This method checks for available drones and fire events, assigns drones to events,
     * and sends tasks to the drones.
     *
     * @return a String representing the task assigned, or "NO_TASK_AVAILABLE" if none is assigned
     * @throws IOException if an I/O error occurs during task assignment
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized String assignTaskToDrone() throws IOException, InterruptedException {
        if ((drones.getAvailableDrones() == 0)) {
            System.out.println("[" + Thread.currentThread().getName() + "]: NO DRONES AVAILABLE || DRONES UNAVAILABLE AT BASE. Going to wait...");
            drones.stopAssigningDrones();
            Scheduler.shouldHandleStuckDrones = true;
        }

        if (eventList.isListEmpty()) {
            System.out.println("[" + Thread.currentThread().getName() + "]: NO EVENTS. Going to wait...");
            eventList.stopAssigningDrones();
            Scheduler.shouldHandleStuckDrones = true;
        }

        System.out.println("[" + Thread.currentThread().getName() + "]: coming out of wait...");
        int dronesAssigned = 0;
        String task = "";
        System.out.println("[" + Thread.currentThread().getName() + "]: Assigning task to drone(s)...");

        if (!eventList.isListEmpty()) {
            FireIncidentEvent fireEvent = eventList.removeEvent();
            if (fireEvent.getWaterNeeded() > 0) {

                System.out.println("[" + Thread.currentThread().getName() + "]: processing fire event " + fireEvent.getZone());
                String event = fireEvent.toString();
                int dronesRequired = calculateDroneRequired(fireEvent);
                System.out.println("[" + Thread.currentThread().getName() + "]: fire event " + fireEvent.getZone() + " requires " + dronesRequired + " drones");

                for (int i = 0; i < drones.getAvailableDrones(); i++) {
                    DroneData closestDroneToZone = findClosestDrone(fireEvent);
                    if (closestDroneToZone == null) {
                        eventList.addEvent(fireEvent);
                        System.out.println("[" + Thread.currentThread().getName() + "]: Drone found NULL. Rechecking...");
                        return "";
                    }

                    int waterReq = Integer.MIN_VALUE;
                    task = sendTaskToDrone(closestDroneToZone, fireEvent);
                    if (fireEvent.getWaterNeeded() >= MAX_PAYLOAD) {
                        waterReq = fireEvent.getWaterNeeded() - MAX_PAYLOAD;
                    } else {
                        waterReq = fireEvent.getWaterNeeded();
                        fireEvent.setWaterNeeded(0);
                    }
                    fireEvent.setWaterNeeded(waterReq);
                    dronesAssigned++;
                    int travelTime = calculateTravelTime(fireEvent.getZone().getCenter());

                    closestDroneToZone.setTravelTime(travelTime);
                    sendResponse(task, closestDroneToZone.getAddress(), closestDroneToZone.getPort());
                    System.out.println("[" + Thread.currentThread().getName() + "]: Drone #" + closestDroneToZone.getDroneID() + " for fire event" + fireEvent);
                    drones.trackDrones(((travelTime + 10) / 10), closestDroneToZone);

                    if (!fireEvent.getFault().equals("NO_FAULT")) {
                        System.out.println("[" + Thread.currentThread().getName() + "]: Fault: " + fireEvent.getFault() + " accounted for drone #" + closestDroneToZone.getDroneID());
                        System.out.println("[" + Thread.currentThread().getName() + "]: Now removing Fault from fire event " + fireEvent + " for other Drones (if assigned).");
                        fireEvent.setFault("NO_FAULT");
                        fireEvent.setFaultType("N/A");
                    }

                    inProgressEvents.addInProgressEvent(closestDroneToZone.getDroneID(), fireEvent);
                    if (dronesRequired == dronesAssigned) {
                        break;
                    }
                }
                if (dronesAssigned != dronesRequired) {
                    eventList.addEvent(fireEvent);
                }
                System.out.println("DRONEDISPATCHED : " + fireEvent.getID());
                metricsLogger.logDroneDispatched(fireEvent.getID());
                stateMachine.getCurrentState().assignTask(stateMachine, this);
                notifyAll();
                return task;
            }
        }
        System.out.println("[" + Thread.currentThread().getName() + "]: No fire events to process at the moment.");
        notifyAll();
        stateMachine.getCurrentState().waitForDrones(stateMachine, this);
        return "NO_TASK_AVAILABLE";
    }

    /**
     * Checks all drones for lateness and marks late drones as lost.
     * If a drone is lost or its nozzle is jammed, the corresponding fire event is requeued.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized void HandleStuckDrones() throws InterruptedException {
        int maxTravelTime = Integer.MIN_VALUE;
        List<Integer> removedDrones = new ArrayList<>();
        List<Integer> nozzleJammedDrones = new ArrayList<>();

        if (inProgressEvents.isInProgressEventListEmpty()) {
            inProgressEvents.isInProgressEventsListEmpty();
            System.out.println("[" + Thread.currentThread().getName() + "]: coming out of wait...");
        }
        // Making a copy of the in-progress event keys to overcome concurrent modification issues.
        Collection<Integer> keys = inProgressEvents.getKeys();
        Iterator<Integer> itr = keys.iterator();
        while (itr.hasNext()) {
            int droneID = itr.next();
            DroneData drone = drones.getADrone(droneID);
            if (drone.isLost && drone.droneJobStatus == -1) {
                eventList.notifyScheduler();
                FireIncidentEvent fireEvent = inProgressEvents.getAnEventInProgress(drone.getDroneID());
                fireEvent.setFault("NO_FAULT");
                fireEvent.setFaultType("N/A");
                System.out.println(fireEvent);
                eventList.addEvent(fireEvent);
                removedDrones.add(droneID);
                System.out.println("[" + Thread.currentThread().getName() + "]: added event " + fireEvent + " again because drone is lost (repeats not permitted)");
            } else if (drone.droneJobStatus == 1 && drone.isComingBack == -1) {
                eventList.notifyScheduler();
                FireIncidentEvent fireEvent = inProgressEvents.getAnEventInProgress(drone.getDroneID());
                fireEvent.setFault("NO_FAULT");
                fireEvent.setFaultType("N/A");
                System.out.println(fireEvent);
                eventList.addEvent(fireEvent);
                nozzleJammedDrones.add(droneID);
                System.out.println("[" + Thread.currentThread().getName() + "]: added event " + fireEvent + " again because drone is lost (repeats not permitted)");
            } else {
                maxTravelTime = Math.max(maxTravelTime, drone.getTravelTime());
            }
        }

        for (int ID : removedDrones) {
            drones.removeDrone(ID);
            inProgressEvents.removeInProgressEvent(ID);
        }
        for (int ID : nozzleJammedDrones) {
            drones.removeDrone(ID);
            inProgressEvents.removeInProgressEvent(ID);
        }
        removedDrones.clear();
        nozzleJammedDrones.clear();

        notifyAll();
        stateMachine.getCurrentState().handleFault(stateMachine, this);
        stateMachine.getCurrentState().returnToIdle(stateMachine, this);
        return;
    }

    /**
     * Calculates the number of drones required to extinguish the given fire event.
     *
     * @param fireEvent the fire incident event
     * @return the number of drones required, calculated as the ceiling of waterNeeded divided by MAX_PAYLOAD
     */
    private int calculateDroneRequired(FireIncidentEvent fireEvent) {
        int waterNeeded = fireEvent.getWaterNeeded();
        int droneRequired = (int) Math.ceil((double) waterNeeded / MAX_PAYLOAD);
        return droneRequired;
    }

    /**
     * Formats and returns a task string for the given drone and fire event.
     *
     * @param closestDroneToZone the drone closest to the fire event zone
     * @param fireEvent          the fire incident event to be assigned
     * @return a task string in the format "droneID:waterNeeded:XCoord:YCoord:fault:faultType"
     * @throws IOException if an I/O error occurs
     */
    public String sendTaskToDrone(DroneData closestDroneToZone, FireIncidentEvent fireEvent) throws IOException {
        int XCoord = (int) fireEvent.getZone().getCenter().getX();
        int YCoord = (int) fireEvent.getZone().getCenter().getY();
        String fault = fireEvent.getFault();
        String faultType = fireEvent.getFaultType();
        return closestDroneToZone.getDroneID() + ":" + fireEvent.getWaterNeeded() + ":" + XCoord + ":" + YCoord + ":" + fault + ":" + faultType;
    }

    /**
     * Finds and returns the drone closest to the fire event.
     * Only drones that have already requested and are at base are considered.
     *
     * @param fireEvent the fire incident event
     * @return the DroneData of the closest available drone, or null if none found
     */
    public synchronized DroneData findClosestDrone(FireIncidentEvent fireEvent) {
        DroneData closestDrone = null;
        double min = Double.MAX_VALUE;
        for (DroneData drone : drones.getDrones()) {
            System.out.println("Has drone #" + drone.getDroneID() + " requested: " + drone.getHasRequested());
            if (drone.getHasRequested() && drone.isAtBase()) {
                double distance = Math.sqrt(Math.pow(fireEvent.getZone().getCenter().getX() - drone.getLocation().getX(), 2) +
                        Math.pow(fireEvent.getZone().getCenter().getY() - drone.getLocation().getY(), 2));

                if (distance < min) {
                    min = distance;
                    closestDrone = drone;
                    closestDrone.setHasRequested(false);
                    closestDrone.setEventID(fireEvent.getZoneId());
                    System.out.println("[" + Thread.currentThread().getName() + "]: closest drone near fire event " + fireEvent.getZone() + " is drone #" + closestDrone.getDroneID());
                }
            }
        }
        if (closestDrone == null) {
            return null;
        }
        closestDrone.setAtBase(false);
        closestDrone.setDroneJobStatus(0);
        closestDrone.setIsComingBack(0);
        closestDrone.setHasArrived(false);
        closestDrone.setCompletedJob(false);
        return closestDrone;
    }

    /**
     * Transforms a received string array into a FireIncidentEvent object.
     *
     * @param parts the string array received from the fire incident subsystem
     * @return a new FireIncidentEvent object created from the string data
     */
    private static FireIncidentEvent getFireIncidentEvent(String[] parts) {
        int zoneId = Integer.parseInt(parts[1].trim());
        String[] eventDetails = (parts[2].split(","));
        String time = "" + parts[0] + ":" + parts[1] + ":" + eventDetails[0];

        int ID = Integer.parseInt(eventDetails[1]);
        String eventType = eventDetails[2];
        String severity = eventDetails[3];
        int waterNeeded = Integer.parseInt(eventDetails[4]);
        String fault = eventDetails[5];
        String faultType = eventDetails[6];

        int startX = Integer.parseInt(eventDetails[9].trim());
        int startY = Integer.parseInt(eventDetails[10].trim());
        int endX = Integer.parseInt(eventDetails[11].trim());
        int endY = Integer.parseInt(eventDetails[12].trim());

        Zone zone = new Zone(zoneId, startX, startY, endX, endY);

        FireIncidentEvent event = new FireIncidentEvent(time, zoneId, eventType, severity, zone, waterNeeded, fault, faultType);
        event.setID(getNextEventID());

        return event;
    }

    /**
     * Processes an incoming drone request by parsing the request, performing the necessary
     * actions, and sending back an appropriate response.
     *
     * @return the response string to be sent back to the drone
     * @throws IOException if an I/O error occurs during processing
     */
    private String processDroneRequest() throws IOException {
        if (!bufferReq.isEmpty()) {
            DatagramPacket packet = bufferReq.removeRequest();
            String request = new String(packet.getData(), 0, packet.getLength());
            String[] parts = request.split(":");
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();
            if (!checkCorrupted(parts)) {
                switch (parts[0]) {
                    case "INITIALIZE_DRONE":
                        return handleInitializeDrone(clientAddress, clientPort);
                    case "GET_TASK":
                        int droneID = Integer.parseInt(parts[1]);
                        return handleGetTask(Integer.parseInt(parts[1]));
                    case "RETURN_TO_SCHEDULER":
                        return handleReturnToScheduler(Integer.parseInt(parts[1]), clientAddress, clientPort);
                    case "NOTIFY_ARRIVED":
                        droneID = Integer.parseInt(parts[1]);
                        return handleArrivedAtDestination(droneID, clientAddress, clientPort);
                    case "OPEN_NOZZLE":
                        droneID = Integer.parseInt(parts[1]);
                        return handleJobCompleted(droneID, clientAddress, clientPort);
                    case "UPDATE_LOCATION":
                        droneID = Integer.parseInt(parts[1]);
                        return handleUpdateDroneLocation(droneID, parts[2], parts[3], clientAddress, clientPort);
                    default:
                        droneID = Integer.parseInt(parts[1]);
                        System.out.println("[" + Thread.currentThread().getName() + "]: received unfamiliar request: " + request + " from droneID#" + droneID);
                        stateMachine.getCurrentState().handleFault(stateMachine, this);
                        requestRetransmission(request, clientAddress, clientPort);
                }
            } else {
                requestRetransmission(request, clientAddress, clientPort);
            }
        }
        return "";
    }

    /**
     * Updates the stored location for a drone and returns an acknowledgement.
     *
     * @param droneID the ID of the drone sending its location
     * @param x       the new x-coordinate as a string
     * @param y       the new y-coordinate as a string
     * @param clientAddress the sender's InetAddress
     * @param clientPort    the sender's port number
     * @return a "Location_OK" acknowledgement message
     */
    private String handleUpdateDroneLocation(int droneID, String x, String y, InetAddress clientAddress, int clientPort) {
        drones.updateLocation(droneID, new Point(Integer.parseInt(x), Integer.parseInt(y)));
        return "Location_OK";
    }

    /**
     * Handles the drone's job completion notification.
     *
     * @param droneID       the ID of the drone that completed its job
     * @param clientAddress the sender's InetAddress
     * @param clientPort    the sender's port number
     * @return an acknowledgement message ("OK")
     */
    private String handleJobCompleted(int droneID, InetAddress clientAddress, int clientPort) {
        String response = "OK";
        DroneData drone = drones.getADrone(droneID);
        return response;
    }

    /**
     * Handles the drone's arrival at its destination.
     *
     * @param droneID       the ID of the arriving drone
     * @param clientAddress the sender's InetAddress
     * @param clientPort    the sender's port number
     * @return a response message ("OPEN_NOZZLE")
     * @throws IOException if an I/O error occurs when sending a response
     */
    private synchronized String handleArrivedAtDestination(int droneID, InetAddress clientAddress, int clientPort) throws IOException {
        System.out.println("[" + Thread.currentThread().getName() + "] -> Drone Data: drone#" + droneID + " has arrived at destination");
        String response = "OPEN_NOZZLE";
        sendResponse(response, clientAddress, clientPort);
        DroneData drone = drones.getADrone(droneID);
        drone.setHasArrived(true);
        drone.setDroneJobStatus(-1);
        int travelTime = drone.getTravelTime();
        int travelDelay = 3;
        drones.trackDroneNozzle(travelDelay, drone);
        return response;
    }

    /**
     * Sends a retransmission request for a corrupted message.
     *
     * @param request       the original request string
     * @param clientAddress the sender's InetAddress
     * @param clientPort    the sender's port number
     * @throws IOException if an I/O error occurs while sending the response
     */
    private synchronized void requestRetransmission(String request, InetAddress clientAddress, int clientPort) throws IOException {
        System.out.println("[" + Thread.currentThread().getName() + "]: handling corrupted request");
        String response = "CORRUPTED_MESSAGED";
        sendResponse(response, clientAddress, clientPort);
    }

    /**
     * Processes a fire request from the fire incident subsystem and transitions the scheduler state accordingly.
     *
     * @param request       the received fire incident request string
     * @param clientAddress the sender's InetAddress
     * @param clientPort    the sender's port number
     * @return a response string after handling the fire event
     * @throws IOException if an I/O error occurs during processing
     */
    private String processFireRequest(String request, InetAddress clientAddress, int clientPort) throws IOException {
        String[] parts = request.split(":");
        stateMachine.getCurrentState().processFireRequest(stateMachine, this);
        stateMachine.getCurrentState().assignTask(stateMachine, this);
        return handleFireEvent(parts, clientAddress, clientPort);
    }

    /**
     * Queues a received fire event and sends an acknowledgement.
     *
     * @param parts   the parsed components of the fire event request
     * @param address the sender's InetAddress
     * @param port    the sender's port number
     * @return an acknowledgement message ("FIRE_EVENT:RECEIVED")
     * @throws IOException if an I/O error occurs while sending the response
     */
    public synchronized String handleFireEvent(String[] parts, InetAddress address, int port) throws IOException {
        FireIncidentEvent event = getFireIncidentEvent(parts);
        eventList.addEvent(event);
        metricsLogger.logFireReported(event.getID(), event.getZoneId());
        System.out.println("LOGFIREREPORTED : fireid: " + event.getID() + ", zoneid: " + event.getZoneId());
        System.out.println("LOGFIREREPORTED : metrics map:" + metricsLogger.getFireEventMetricsMap().toString());
        numbFireEvents = numbFireEvents + 1;
        System.out.println("Number of fire events added by now: " + numbFireEvents + " with currently " + eventList.getListSize() + " in processing \n");
        String response = "FIRE_EVENT:RECEIVED";
        sendResponse(response, address, port);
        drones.checkEvents();
        System.out.println("[DEBUG]: NOTIFYALL");
        return response;
    }

    /**
     * Handles the initialization of a new drone. A unique drone ID is assigned and sent back.
     *
     * @param address the requesting drone's InetAddress
     * @param port    the requesting drone's port number
     * @return the assigned drone ID as a String
     */
    public String handleInitializeDrone(InetAddress address, int port) {
        System.out.println("[" + Thread.currentThread().getName() + "]: Initializing a drone...");
        int assignedID = nextDroneID++;
        DroneData drone = new DroneData(assignedID, new Point(0, 0), MAX_PAYLOAD, address, port);
        drones.addDrone(assignedID, drone);
        System.out.println("[" + Thread.currentThread().getName() + "]: new drone initialized with drone ID #" + assignedID);
        int droneID = drone.getDroneID();
        String droneIDMsg = Integer.toString(droneID);
        System.out.println("[Scheduler]: Drone ID sent back as response");
        try {
            sendResponse(droneIDMsg, address, port);
        } catch (IOException e) {
            // Handle the exception as needed.
        }
        return droneIDMsg;
    }

    /**
     * Handles a GET_TASK request from a drone by marking it as available for a task.
     *
     * @param droneID the ID of the requesting drone
     * @return an empty string (task will be assigned via other mechanisms)
     * @throws IOException if an I/O error occurs during processing
     */
    private String handleGetTask(int droneID) throws IOException {
        DroneData drone = drones.getADrone(droneID);
        drones.isAvailable(droneID);
        drone.setHasRequested(true);
        drones.notifyScheduler();
        return "";
    }

    /**
     * Handles a RETURN_TO_SCHEDULER request from a drone, marking it as no longer busy and logging the completed job.
     *
     * @param droneID       the ID of the returning drone
     * @param address       the sender's InetAddress
     * @param port          the sender's port number
     * @return an acknowledgement message ("RETURN_TO_SCHEDULER:OK")
     */
    private String handleReturnToScheduler(int droneID, InetAddress address, int port) {
        DroneData drone = drones.getADrone(droneID);
        drone.setHasRequested(false);
        drone.setIsComingBack(1);
        String response = "RETURN_TO_SCHEDULER:OK";
        try {
            sendResponse(response, address, port);
        } catch (IOException e) {
            // Handle exception if needed.
        }
        drone.setCompletedJob(true);
        FireIncidentEvent event = inProgressEvents.getAnEventInProgress(droneID);
        System.out.println("FIREEXTINGUISHED : " + event.getID());
        metricsLogger.logFireExtinguished(event.getID());
        FireEventMetrics fireEventMetrics = metricsLogger.getFireEventMetricsMap().get(event.getID());
        if (fireEventMetrics.isComplete()) {
            try {
                metricsLogger.logFireMetrics(fireEventMetrics.fireID, fireEventMetrics.zoneID, fireEventMetrics.getResponseTime(), fireEventMetrics.getExtinguishTime());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        stateMachine.getCurrentState().returnToIdle(stateMachine, this);
        return "RETURN_TO_SCHEDULER:OK";
    }

    /**
     * Sends a response message to a given address and port.
     *
     * @param response the response message to send
     * @param address  the recipient's InetAddress
     * @param port     the recipient's port number
     * @throws IOException if an I/O error occurs while sending the response
     */
    private void sendResponse(String response, InetAddress address, int port) throws IOException {
        byte[] arr = response.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(arr, arr.length, address, port);
        socket.send(responsePacket);
        System.out.println("[" + Thread.currentThread().getName() + "]: Sent Response :" + response);
        stateMachine.getCurrentState().returnToIdle(stateMachine, this);
    }

    /**
     * Sets the current state of the scheduler.
     *
     * @param state the new SchedulerState to set
     */
    public void setState(SchedulerState state) {
        this.state = state;
    }

    /**
     * Listens for incoming drone requests over the DatagramSocket.
     */
    public void handleIncomingDroneRequests() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            bufferReq.addRequest(packet);
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("[" + Thread.currentThread().getName() + "]: received request from drone: " + request);
            processDroneRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Listens for incoming fire incident requests over the DatagramSocket.
     */
    public void handleIncomingFireIncidentRequests() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("[" + Thread.currentThread().getName() + "]: received request from fire incident: " + request);
            String response = processFireRequest(request, packet.getAddress(), packet.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main entry point for the Scheduler thread.
     * Depending on the thread's name, it processes drone requests, fire incident requests,
     * assigns tasks, or tracks drones.
     */
    @Override
    public void run() {
        System.out.println("[" + Thread.currentThread().getName() + "]: Online. \n");
        while (true) {
            if (Thread.currentThread().getName().equals("Drone listener")) {
                handleIncomingDroneRequests();
            } else if (Thread.currentThread().getName().equals("Drone Scheduler")) {
                try {
                    assignTaskToDrone();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else if (Thread.currentThread().getName().equals("Drone Tracker")) {
                try {
                    HandleStuckDrones();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                handleIncomingFireIncidentRequests();
            }
        }
    }

    /**
     * Retrieves and returns a FireIncidentEvent object created from the received string parts.
     *
     * @param parts the array of string parts representing the fire event data
     * @return a new FireIncidentEvent object
     */
    private static FireIncidentEvent getFireIncidentEvent(String[] parts) {
        int zoneId = Integer.parseInt(parts[1].trim());
        String[] eventDetails = (parts[2].split(","));
        String time = "" + parts[0] + ":" + parts[1] + ":" + eventDetails[0];

        int ID = Integer.parseInt(eventDetails[1]);
        String eventType = eventDetails[2];
        String severity = eventDetails[3];
        int waterNeeded = Integer.parseInt(eventDetails[4]);
        String fault = eventDetails[5];
        String faultType = eventDetails[6];

        int startX = Integer.parseInt(eventDetails[9].trim());
        int startY = Integer.parseInt(eventDetails[10].trim());
        int endX = Integer.parseInt(eventDetails[11].trim());
        int endY = Integer.parseInt(eventDetails[12].trim());

        Zone zone = new Zone(zoneId, startX, startY, endX, endY);
        FireIncidentEvent event = new FireIncidentEvent(time, zoneId, eventType, severity, zone, waterNeeded, fault, faultType);
        event.setID(getNextEventID());
        return event;
    }

    /**
     * Processes an incoming fire incident request, updates fire event metrics, and sends an acknowledgement.
     *
     * @param parts   the parsed string parts of the fire incident request
     * @param address the sender's InetAddress
     * @param port    the sender's port number
     * @return an acknowledgement message ("FIRE_EVENT:RECEIVED")
     * @throws IOException if an I/O error occurs while sending the response
     */
    public synchronized String handleFireEvent(String[] parts, InetAddress address, int port) throws IOException {
        FireIncidentEvent event = getFireIncidentEvent(parts);
        eventList.addEvent(event);
        metricsLogger.logFireReported(event.getID(), event.getZoneId());
        System.out.println("LOGFIREREPORTED : fireid: " + event.getID() + ", zoneid: " + event.getZoneId());
        System.out.println("LOGFIREREPORTED : metrics map:" + metricsLogger.getFireEventMetricsMap().toString());
        numbFireEvents = numbFireEvents + 1;
        System.out.println("Number of fire events added by now: " + numbFireEvents + " with currently " + eventList.getListSize() + " in processing \n");
        String response = "FIRE_EVENT:RECEIVED";
        sendResponse(response, address, port);
        drones.checkEvents();
        System.out.println("[DEBUG]: NOTIFYALL");
        return response;
    }

    /**
     * Processes a fire request from the fire incident subsystem.
     * Transitions the scheduler state and then handles the fire event.
     *
     * @param request       the received fire incident request string
     * @param clientAddress the sender's InetAddress
     * @param clientPort    the sender's port number
     * @return a response string after handling the fire event
     * @throws IOException if an I/O error occurs during processing
     */
    private String processFireRequest(String request, InetAddress clientAddress, int clientPort) throws IOException {
        String[] parts = request.split(":");
        stateMachine.getCurrentState().processFireRequest(stateMachine, this);
        stateMachine.getCurrentState().assignTask(stateMachine, this);
        return handleFireEvent(parts, clientAddress, clientPort);
    }

    /**
     * Sends a response back to the original request sender.
     *
     * @param response  the response message to send
     * @param address   the recipient's InetAddress
     * @param port      the recipient's port number
     * @throws IOException if an I/O error occurs during sending
     */
    private void sendResponse(String response, InetAddress address, int port) throws IOException {
        byte[] arr = response.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(arr, arr.length, address, port);
        socket.send(responsePacket);
        System.out.println("[" + Thread.currentThread().getName() + "]: Sent Response :" + response);
        stateMachine.getCurrentState().returnToIdle(stateMachine, this);
    }

    /**
     * Processes incoming drone requests and delegates handling based on the message content.
     *
     * @throws IOException if an I/O error occurs during processing
     */
    public void handleIncomingDroneRequests() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            bufferReq.addRequest(packet);
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("[" + Thread.currentThread().getName() + "]: received request from drone: " + request);
            processDroneRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Processes incoming fire incident requests.
     */
    public void handleIncomingFireIncidentRequests() {
        try {
            byte[] buffer = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String request = new String(packet.getData(), 0, packet.getLength());
            System.out.println("[" + Thread.currentThread().getName() + "]: received request from fire incident: " + request);
            String response = processFireRequest(request, packet.getAddress(), packet.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main run loop for the Scheduler thread. Depending on the thread's name,
     * it processes drone requests, assigns tasks, tracks stuck drones, or handles fire incident requests.
     */
    @Override
    public void run() {
        System.out.println("[" + Thread.currentThread().getName() + "]: Online. \n");
        while (true) {
            if (Thread.currentThread().getName().equals("Drone listener")) {
                handleIncomingDroneRequests();
            } else if (Thread.currentThread().getName().equals("Drone Scheduler")) {
                try {
                    assignTaskToDrone();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else if (Thread.currentThread().getName().equals("Drone Tracker")) {
                try {
                    HandleStuckDrones();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                handleIncomingFireIncidentRequests();
            }
        }
    }

    /**
     * The main entry point of the Scheduler application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FireEventList eventList = new FireEventList();
            DroneRequestBuffer bufferReq = new DroneRequestBuffer();
            DroneFleet drones = new DroneFleet(eventList);
            InProgressEvents inProgressEvents = new InProgressEvents();

            Thread eventScheduler = new Thread(new Scheduler(drones, bufferReq, eventList, inProgressEvents, PORT_NUMBER_FIRE_INCIDENT_SYSTEM), "Event Scheduler");
            Thread droneListener = new Thread(new Scheduler(drones, bufferReq, eventList, inProgressEvents, PORT_NUMBER_DRONE_SYSTEM), "Drone listener");
            Thread droneScheduler = new Thread(new Scheduler(drones, bufferReq, eventList, inProgressEvents, 4000), "Drone Scheduler");
            Thread droneTracker = new Thread(new Scheduler(drones, bufferReq, eventList, inProgressEvents, 9000), "Drone Tracker");

            eventScheduler.start();
            droneListener.start();
            droneScheduler.start();
            droneTracker.start();
        });
    }
}
