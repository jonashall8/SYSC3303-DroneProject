import java.util.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The {@code FireIncidentEvent} class represents a fire incident, storing details such as
 * the event time, affected zone, event type, severity, and water needed for response.
 * Implements {@code Serializable} to allow object persistence.
 *
 * Key functionalities include:
 *   Parsing event time from a formatted string.
 *   Storing details related to fire incidents.
 *   Providing accessor methods for retrieving event data.
 */
public class FireIncidentEvent implements Serializable {
    private int id;
    private String time;
    private int zoneId;
    private String eventType;
    private String severity;
    private Zone zone;
    private int waterNeeded;
    private String fault;
    private String faultType;
    private String taskStatus;  //"New", "In Progress", "Complete"
    private LocalTime eventTime;

    /**
     * Constructs a {@code FireIncidentEvent} object with the specified parameters.
     *
     * @param time        The time of the fire incident in HH:mm:ss format.
     * @param zoneId      The ID of the affected zone.
     * @param eventType   The type of fire incident.
     * @param severity    The severity level of the incident.
     * @param zone        The associated zone object.
     * @param waterNeeded The amount of water required to extinguish the fire.
     */
    public FireIncidentEvent(String time, int zoneId, String eventType, String severity, Zone zone, int waterNeeded, String Fault, String faultType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        this.eventTime = LocalTime.parse(time, formatter);
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.zone = zone;
        this.waterNeeded = waterNeeded;
        this.fault = Fault;
        this.faultType = faultType;
        this.taskStatus = "New";
    }


    public void setID(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    /**
     * Gets the time of the fire incident.
     *
     * @return The time of the fire incident in HH:mm:ss format.
     */
    public String getTime() {
        return time;
    }

    /**
     * Gets the zone ID where the fire incident occurred.
     *
     * @return The zone ID.
     */
    public int getZoneId() {
        return zoneId;
    }

    /**
     * Gets the type of fire incident.
     *
     * @return The event type of the fire incident.
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the severity of the fire incident.
     *
     * @return The severity level of the incident.
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Gets the associated zone object.
     *
     * @return The {@code Zone} object associated with the fire incident.
     */
    public Zone getZone() {
        return zone;
    }

    /**
     * Gets the amount of water needed to extinguish the fire.
     *
     * @return The required water amount.
     */
    public int getWaterNeeded() {
        return waterNeeded;
    }


    public String getFault() {
        return fault;
    }

    public String getFaultType() {
        return faultType;
    }

    public void setFault(String fault) {
        this.fault = fault;
    }

    public void setFaultType(String faultType) {
        this.faultType = faultType;
    }
    /**
     * Gets the timestamp of the event.
     *
     * @return The timestamp of the fire incident.
     */
    public LocalTime getEventTime() {
        return eventTime;
    }

    /**
     * Sets the amount of water needed to extinguish the fire.
     *
     * @param waterNeeded The amount of water needed.
     */
    public void setWaterNeeded(int waterNeeded) {
        this.waterNeeded = waterNeeded;

    }

    @Override
    public String toString() {
        return time + "," + zoneId + "," + eventType + "," + severity + "," + waterNeeded + "," + fault + "," + faultType + "," + taskStatus + "," + zone.toString();

    }


    public static void main(String[] args) {
        // define the path to your test file
        String filePath = "src/Sample_event.src";
        ArrayList<String[]> events = new ArrayList<>();


        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            // skip the header line
            br.readLine();

            // read the file line by line
            while ((line = br.readLine()) != null) {
                // Split the line by commas and trim any extra whitespace
                String[] splitLine = line.split(",");
                for (int i = 0; i < splitLine.length; i++) {
                    splitLine[i] = splitLine[i].trim();
                }
                events.add(splitLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // converts events into array [0] -> [9]

        // convert arrayList to 2D array
        String[][] eventsArray = events.toArray(new String[0][]);



        // print the 2D array for verification
        for (String[] event : eventsArray) {
            for (String element : event) {
                System.out.print(element + " , ");
            }
        }
    }

}
    
    
    
