import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code MetricsLogger} class records and logs fire event metrics, including
 * response time and extinguish time, to a CSV file. It tracks the timeline of each
 * fire incident using {@link FireEventMetrics}.
 */
public class MetricsLogger {
    private BufferedWriter writer;
    private Map<Integer, FireEventMetrics> fireEventMetricsMap;

    /**
     * Constructs a {@code MetricsLogger} instance and initializes the CSV file for writing.
     *
     * @param filename The name of the CSV file to write metrics to.
     * @throws IOException If an I/O error occurs while creating or writing to the file.
     */
    public MetricsLogger(String filename) throws IOException {
        writer = new BufferedWriter(new FileWriter(filename));
        writer.write("FireID,ZoneID,ResponseTime(ms),ExtinguishTime(ms)\n");
        fireEventMetricsMap = new ConcurrentHashMap<>();
    }

    /**
     * Logs a completed fire event with its associated metrics to the CSV file.
     *
     * @param fireID         The ID of the fire incident.
     * @param zoneID         The zone where the fire occurred.
     * @param responseTime   Time (in ms) from fire report to drone dispatch.
     * @param extinguishTime Time (in ms) from fire report to extinguishing.
     * @throws IOException If an error occurs while writing to the file.
     */
    public synchronized void logFireMetrics(int fireID, int zoneID, long responseTime, long extinguishTime) throws IOException {
        writer.write(String.format("%s,%s,%d,%d\n", fireID, zoneID, responseTime, extinguishTime));
        writer.flush();
    }

    /**
     * Closes the file writer associated with this logger.
     *
     * @throws IOException If an error occurs while closing the file.
     */
    public void close() throws IOException {
        writer.close();
    }

    /**
     * Returns the map of all tracked fire event metrics.
     *
     * @return A {@code Map} of fire IDs to {@code FireEventMetrics}.
     */
    public Map<Integer, FireEventMetrics> getFireEventMetricsMap() {
        return fireEventMetricsMap;
    }

    /**
     * Logs the time at which a fire was first reported.
     *
     * @param fireID The ID of the fire.
     * @param zoneID The zone in which the fire occurred.
     */
    public void logFireReported(int fireID, int zoneID) {
        fireEventMetricsMap.put(fireID, new FireEventMetrics(fireID, zoneID));
        fireEventMetricsMap.get(fireID).fireReportedTime = System.currentTimeMillis();
    }

    /**
     * Logs the time at which a drone was dispatched to the fire.
     *
     * @param fireID The ID of the fire.
     */
    public void logDroneDispatched(int fireID) {
        fireEventMetricsMap.get(fireID).droneDispatchTime = System.currentTimeMillis();
    }

    /**
     * Logs the time at which the fire was extinguished.
     *
     * @param fireID The ID of the fire.
     */
    public void logFireExtinguished(int fireID) {
        fireEventMetricsMap.get(fireID).fireExtinguishedTime = System.currentTimeMillis();
    }
}

/**
 * The {@code FireEventMetrics} class stores timestamps for fire lifecycle events:
 * when it was reported, when a drone was dispatched, and when it was extinguished.
 */
class FireEventMetrics {
    public int fireID;
    public int zoneID;
    public long fireReportedTime = -1;
    public long droneDispatchTime = -1;
    public long fireExtinguishedTime = -1;

    /**
     * Constructs a {@code FireEventMetrics} instance for a fire event.
     *
     * @param fireID The ID of the fire.
     * @param zoneID The zone in which the fire occurred.
     */
    public FireEventMetrics(int fireID, int zoneID) {
        this.fireID = fireID;
        this.zoneID = zoneID;
    }

    /**
     * Checks if all timestamps have been recorded for the fire event.
     *
     * @return {@code true} if all events (reported, dispatched, extinguished) have been logged.
     */
    public boolean isComplete() {
        return fireReportedTime > 0 && droneDispatchTime > 0 && fireExtinguishedTime > 0;
    }

    /**
     * Calculates the response time between fire reported and drone dispatched.
     *
     * @return The response time in milliseconds.
     */
    public long getResponseTime() {
        return droneDispatchTime - fireReportedTime;
    }

    /**
     * Calculates the extinguish time between fire reported and extinguished.
     *
     * @return The extinguish time in milliseconds.
     */
    public long getExtinguishTime() {
        return fireExtinguishedTime - fireReportedTime;
    }
}
