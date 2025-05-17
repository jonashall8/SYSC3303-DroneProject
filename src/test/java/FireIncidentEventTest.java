import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


public class FireIncidentEventTest {
    private static final String TEST_FILE = "src/main/resources/Sample_event_file.csv";

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary test file in src/main/resources
        List<String> lines = List.of(
                "Time,Zone ID,Event type,Severity,Water Needed",
                "14:03:15,1,FIRE_DETECTED,High,500",
                "14:10:00,2,DRONE_REQUEST,Moderate,300"
        );
        Files.write(Paths.get(TEST_FILE), lines);
    }

    @Test
    void testFireIncidentEventCreation() {
        Zone mockZone = new Zone(1,0,0, 700, 600); // Assuming Zone has this constructor
        FireIncidentEvent event = new FireIncidentEvent("14:03:15", 1, "FIRE_DETECTED", "High", mockZone, 500, "", "");

        assertEquals("14:03:15", event.getTime());
        assertEquals(1, event.getZoneId());
        assertEquals("FIRE_DETECTED", event.getEventType());
        assertEquals("High", event.getSeverity());
        assertEquals(mockZone, event.getZone());
        assertEquals(500, event.getWaterNeeded());
        assertEquals(LocalTime.of(14, 3, 15), event.getEventTime());
    }

    @Test
    void testCSVReading() throws IOException {
        List<FireIncidentEvent> fireIncidents = loadFireIncidents(TEST_FILE);
        assertEquals(2, fireIncidents.size());

        FireIncidentEvent firstEvent = fireIncidents.get(0);
        assertEquals("14:03:15", firstEvent.getTime());
        assertEquals(1, firstEvent.getZoneId());
        assertEquals("FIRE_DETECTED", firstEvent.getEventType());
        assertEquals("High", firstEvent.getSeverity());
        assertEquals(500, firstEvent.getWaterNeeded());

        FireIncidentEvent secondEvent = fireIncidents.get(1);
        assertEquals("14:10:00", secondEvent.getTime());
        assertEquals(2, secondEvent.getZoneId());
        assertEquals("DRONE_REQUEST", secondEvent.getEventType());
        assertEquals("Moderate", secondEvent.getSeverity());
        assertEquals(300, secondEvent.getWaterNeeded());
    }

    @Test
    void testMissingFileHandling() {
        assertThrows(IOException.class, () -> loadFireIncidents("invalid_file.csv"));
    }

    private List<FireIncidentEvent> loadFireIncidents(String filePath) throws IOException {
        List<FireIncidentEvent> fireIncidents = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // Skip header
            String line;

            while ((line = br.readLine()) != null) {
                String[] splitLine = line.split(",");
                if (splitLine.length < 5) continue; // Ensure correct columns

                String time = splitLine[0].trim();
                int zoneId = Integer.parseInt(splitLine[1].trim());
                String eventType = splitLine[2].trim();
                String severity = splitLine[3].trim();
                int waterNeeded = Integer.parseInt(splitLine[4].trim());

                Zone zone = new Zone(zoneId, 0,600,100, 100); // Mock zone object
                fireIncidents.add(new FireIncidentEvent(time, zoneId, eventType, severity, zone, waterNeeded, "", ""));
            }
        }
        return fireIncidents;
    }
}

 
