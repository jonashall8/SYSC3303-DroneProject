import java.io.*;
import java.net.*;
import java.util.*;
import java.time.temporal.*;

/**
 * The {@code FireIncidentSubSystem} class is responsible for managing fire incidents
 * by loading fire event data, processing them sequentially, and forwarding incidents
 * to the scheduler via UDP.
 */
public class FireIncidentSubSystem {
    private Map<Integer, Zone> zones;
    private static final int TIME_SCALE = 10; // Scale time (for sleep calls) by this number

    // UDP Declarations
    private DatagramSocket socket;
    private InetAddress schedulerAddr;
    private static final String SCHEDULER_IP = "127.0.0.1";
    private static final int SCHEDULER_PORT = 6000;

    List<FireIncidentEvent> fireIncidents;

    /**
     * Constructs a {@code FireIncidentSubSystem} instance with file paths.
     *
     * @param filepath_z The path to the zone data file.
     * @param filepath_d The path to the fire incident data file.
     */
    public FireIncidentSubSystem(String filepath_z, String filepath_d) {
        this.zones = loadZonesFromFile(filepath_z);
        fireIncidents = loadFireIncidents(filepath_d);

        try {
            socket = new DatagramSocket();
            schedulerAddr = InetAddress.getByName(SCHEDULER_IP);
            System.out.println("Connection Established with Scheduler: " + SCHEDULER_IP + ":" + SCHEDULER_PORT);
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * Loads zones from a file and returns a map of zone objects.
     *
     * @param filePath The path to the zone data file.
     * @return A map containing zone IDs as keys and corresponding Zone objects as values.
     */
    private Map<Integer, Zone> loadZonesFromFile(String filePath) {
        Map<Integer, Zone> zones = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header line

            while ((line = br.readLine()) != null) {
                String[] splitLine = line.split(",");

                if (splitLine.length < 3) {
                    System.out.println("[Error] Malformed zone line: " + line);
                    continue;
                }

                try {
                    int zoneId = Integer.parseInt(splitLine[0].trim());

                    // Extract and parse coordinates from (x;y) format
                    String[] startCoords = splitLine[1].replaceAll("[()]", "").split(";");
                    String[] endCoords = splitLine[2].replaceAll("[()]", "").split(";");

                    if (startCoords.length != 2 || endCoords.length != 2) {
                        System.out.println("[Error] Invalid coordinate format in zone line: " + line);
                        continue;
                    }

                    int startX = Integer.parseInt(startCoords[0].trim());
                    int startY = Integer.parseInt(startCoords[1].trim());
                    int endX = Integer.parseInt(endCoords[0].trim());
                    int endY = Integer.parseInt(endCoords[1].trim());

                    zones.put(zoneId, new Zone(zoneId, startX, startY, endX, endY));
                } catch (NumberFormatException e) {
                    System.out.println("[Error] Invalid number format in zone line: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("[Error] Unable to read zone file: " + filePath);
            e.printStackTrace();
        }
        System.out.println("Loaded Zones: " + zones.keySet());
        return zones;

    }

    /**
     * Loads fire incidents from a file and returns a list of fire incident events.
     *
     * @param filePath The path to the fire incident data file.
     * @return A list of {@code FireIncidentEvent} objects representing fire incidents.
     */
    public List<FireIncidentEvent> loadFireIncidents(String filePath) {
        List<FireIncidentEvent> incidents = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // Skip header line


            while ((line = br.readLine()) != null) {
                line = line.trim(); // Remove leading/trailing spaces
                if (line.isEmpty()) continue; // Skip empty lines

                String[] splitLine = line.split(",");
                if (splitLine.length < 6) {
                    System.out.println("[Error] Malformed fire incident line: " + line);
                    continue;
                }

                try {
                    String time = splitLine[0].trim();
                    int zoneId = Integer.parseInt(splitLine[1].trim()); // Parse Zone ID
                    String eventType = splitLine[2].trim();
                    String severity = splitLine[3].trim();
                    String fault = splitLine[4].trim();
                    String faultType = splitLine[5].trim();

                    Zone zone = zones.get(zoneId);
                    if (zone != null) {
                        int waterNeeded = 0;
                        switch (severity) {
                            case "High" -> waterNeeded = 30;
                            case "Moderate" -> waterNeeded = 20;
                            case "Low" -> waterNeeded = 10;
                            default -> System.out.println("[Error] Unknown severity: " + severity);
                        }
                        FireIncidentEvent event = new FireIncidentEvent(time, zoneId, eventType, severity, zone, waterNeeded, fault, faultType);

                        incidents.add(event);
                    } else {
                        System.out.println("[Warning] Zone ID " + zoneId + " not found in system: " + line);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("[Error] Invalid number format for Zone ID in line: " + line);
                } catch (Exception e) {
                    System.out.println("[Error] Failed to parse fire incident line: " + line);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.out.println("[Error] Unable to read fire incident file: " + filePath);
            e.printStackTrace();
        }

        // System.out.println("DEBUG: 1st fire incident: " + incidents.getFirst().getTime() + " " + incidents.getFirst().getZoneId() + " " + incidents.getFirst().getEventType() + " " + incidents.getFirst().getSeverity() + " " + incidents.getFirst().getZone() + " " + incidents.getFirst().getWaterNeeded());
        //System.out.println("DEBUG: 1st fire incident: " + incidents.get(1).getTime() + " " + incidents.get(1).getZoneId() + " " + incidents.get(1).getEventType() + " " + incidents.get(1).getSeverity() + " " + incidents.get(1).getZone() + " " + incidents.get(1).getWaterNeeded());

        return incidents;
    }
    /**
     * Processes fire incidents sequentially and sends them to the scheduler.
     */
    public void processFireIncidents() {
        if (fireIncidents.isEmpty()) {
            System.out.println("[Error] No fire incidents found.");
            return;
        }

        FireIncidentEvent previousEvent = null;

        for (FireIncidentEvent event : fireIncidents) {
            if (previousEvent != null) {
                long timeDifference = ChronoUnit.SECONDS.between(previousEvent.getEventTime(), event.getEventTime());
                if (timeDifference > 0) {
                    try {
                        Thread.sleep((timeDifference * 1000) / TIME_SCALE);
                    } catch (InterruptedException e) {
                        System.out.println("[Error] Interrupted while waiting for next event.");
                        e.printStackTrace();
                    }
                }
            }
            sendRequest(event.toString());
            previousEvent = event;
        }
    }

    /**
     * Sends a fire event to the scheduler.
     */
    private void sendRequest(String request) {
        try {
            byte[] outData = request.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(outData, outData.length, schedulerAddr, SCHEDULER_PORT);
            socket.send(sendPacket);
            System.out.println("FireSubSystem: Sent \"" + request + "\" to " + schedulerAddr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to start the FireIncidentSubSystem.
     */
    public static void main(String[] args) {
        FireIncidentSubSystem fireSystem = new FireIncidentSubSystem("src/main/resources/Sample_zone_file.csv", "src/main/resources/Sample_event_file.csv");
        fireSystem.processFireIncidents();
    }
}

