import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.List;

/**
 * FirefightingDroneGridGUI is the main graphical user interface for the firefighting drone system.
 * It displays a grid representing zones, shows fires and drone positions, and includes panels for
 * status updates and drone information.
 */
public class FirefightingDroneGridGUI extends JFrame {
    /** Number of rows in the grid. */
    private static final int GRID_ROWS = 2;
    /** Number of columns in the grid. */
    private static final int GRID_COLS = 4;
    /** The size (in pixels) of each cell in the grid. */
    private static final int CELL_SIZE = 200; // Each cell is 200x200 pixels

    /**
     * Map from grid point (zone) to count of fire events in that zone.
     * A nonzero count indicates that the zone is on fire.
     */
    private Map<Point, Integer> fireZones = new HashMap<>();

    /** Shared DroneFleet instance. */
    private DroneFleet droneFleet;
    /** Set to keep track of processed events (to avoid duplicates). */
    private Set<String> processedEvents = new HashSet<>();
    /** Scheduler address. */
    public static InetAddress schedulerAddr;
    /** Scheduler IP. */
    private static final String SCHEDULER_IP = "127.0.0.1";
    /**
     * Map to store each drone's assigned target (grid coordinates).
     * Key: Drone ID; Value: grid point target.
     */
    private Map<Integer, Point> droneTargets = new HashMap<>();
    /**
     * Map to track which drone is assigned to which fire zone (grid coordinate).
     * Key: grid point (zone); Value: Drone ID.
     */
    private Map<Point, Integer> assignedDrones = new HashMap<>();

    /** Water needed to extinguish the fire event (parsed from DRONE_REQUEST event). */
    private int waterNeeded;
    /** Flag indicating whether the current DRONE_REQUEST event has a fault. */
    private boolean hasFault;

    /** Text area to log status updates on the right side of the UI. */
    private JTextArea updateLog;

    /**
     * Constructs a new FirefightingDroneGridGUI.
     *
     * @param droneFleet the shared DroneFleet instance
     */
    public FirefightingDroneGridGUI(DroneFleet droneFleet) {
        setTitle("Firefighting Drone System");
        setSize(GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE + 50);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        this.droneFleet = droneFleet;

        // Set layout with a grid panel in the center, drone info on the left, and update log on the right.
        setLayout(new BorderLayout());

        // Create the grid panel.
        FireGridPanel gridPanel = new FireGridPanel();
        gridPanel.setPreferredSize(new Dimension(GRID_COLS * CELL_SIZE, GRID_ROWS * CELL_SIZE));
        add(gridPanel, BorderLayout.CENTER);

        // Create the update log area.
        updateLog = new JTextArea();
        updateLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(updateLog);
        scrollPane.setPreferredSize(new Dimension(300, GRID_ROWS * CELL_SIZE));
        add(scrollPane, BorderLayout.EAST);

        // Create the drone info panel.
        DroneInfoPanel statsPanel = new DroneInfoPanel();
        add(statsPanel, BorderLayout.WEST);

        // Use a Swing Timer to periodically repaint the grid and refresh drone info.
        Timer timer = new Timer(1000, e -> {
            gridPanel.repaint();
            statsPanel.refreshDroneInfo();
        });
        timer.start();

        pack();
    }

    /**
     * Appends a status update message to the update log area.
     *
     * @param msg the message to log
     */
    private void logUpdate(String msg) {
        SwingUtilities.invokeLater(() -> {
            updateLog.append(msg + "\n");
            // Auto-scroll to the bottom.
            updateLog.setCaretPosition(updateLog.getDocument().getLength());
        });
    }

    /**
     * DroneInfoPanel displays status information about each drone and the fire status of each zone.
     */
    private class DroneInfoPanel extends JPanel {
        private JTextArea droneInfoText;

        /**
         * Constructs a new DroneInfoPanel.
         */
        public DroneInfoPanel() {
            setBackground(Color.WHITE);
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(200, GRID_ROWS * CELL_SIZE));

            droneInfoText = new JTextArea();
            droneInfoText.setEditable(false);
            droneInfoText.setFont(new Font("Monospaced", Font.PLAIN, 14));

            JScrollPane scrollPane = new JScrollPane(droneInfoText);
            add(scrollPane, BorderLayout.CENTER);

            // Refresh drone info every 2 seconds.
            new Timer(2000, e -> refreshDroneInfo()).start();
        }

        /**
         * Refreshes the drone information display.
         */
        private void refreshDroneInfo() {
            StringBuilder info = new StringBuilder();
            info.append("\nDrone Statuses:\n");
            for (DroneData drone : droneFleet.getDrones()) {
                info.append("Drone ").append(drone.getDroneID());
                if (droneTargets.containsKey(drone.getDroneID())) {
                    info.append(" Assigned");
                } else {
                    info.append(" Unassigned");
                }
                info.append(" | Water: ").append(drone.getAvailableWater()).append("\n");
            }
            info.append("\nZone Fire Status:\n");
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    if (row == 0 && col == 0) {
                        continue; // Skip Drone Station.
                    }
                    Point zone = new Point(col, row);
                    String label = "Zone " + (row * GRID_COLS + col);
                    boolean onFire = fireZones.containsKey(zone);
                    info.append(label)
                            .append(": ")
                            .append(onFire ? "FIRE" : "NO FIRE")
                            .append("\n");
                }
            }
            droneInfoText.setText(info.toString());
        }
    }

    /**
     * FireGridPanel draws the grid, fire zones, and drones.
     */
    private class FireGridPanel extends JPanel {
        /** Forest green color for the zone backgrounds. */
        private final Color FOREST_GREEN = new Color(34, 139, 34);

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Draw grid cells.
            for (int row = 0; row < GRID_ROWS; row++) {
                for (int col = 0; col < GRID_COLS; col++) {
                    int x = col * CELL_SIZE;
                    int y = row * CELL_SIZE;
                    // The top-left cell (0,0) is the Drone Station.
                    if (row == 0 && col == 0) {
                        g.setColor(Color.LIGHT_GRAY);
                    } else {
                        g.setColor(FOREST_GREEN);
                    }
                    g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                    // Draw a label for the zone.
                    String zoneLabel = (row == 0 && col == 0) ? "Drone Station" : "Zone " + (row * GRID_COLS + col);
                    g.drawString(zoneLabel, x + 10, y + 20);
                }
            }

            // Draw fire circles for zones with fires.
            int fireSize = 20; // Diameter for fire circles.
            for (Map.Entry<Point, Integer> entry : fireZones.entrySet()) {
                Point gridPoint = entry.getKey();
                int count = entry.getValue();
                int cellX = gridPoint.x * CELL_SIZE;
                int cellY = gridPoint.y * CELL_SIZE;
                // Distribute fire circles evenly within the cell.
                int cols = (int) Math.ceil(Math.sqrt(count));
                int rows = (int) Math.ceil((double) count / cols);
                int cellWidth = CELL_SIZE / cols;
                int cellHeight = CELL_SIZE / rows;
                for (int i = 0; i < count; i++) {
                    int colIndex = i % cols;
                    int rowIndex = i / cols;
                    int fireX = cellX + colIndex * cellWidth + cellWidth / 2 - fireSize / 2;
                    int fireY = cellY + rowIndex * cellHeight + cellHeight / 2 - fireSize / 2;
                    g.setColor(Color.RED);
                    g.fillOval(fireX, fireY, fireSize, fireSize);
                }
            }

            // Draw drones on top.
            for (DroneData drone : droneFleet.getDrones()) {
                Point loc = drone.getLocation();
                int droneX = loc.x * CELL_SIZE + CELL_SIZE / 2 - 5;
                int droneY = loc.y * CELL_SIZE + CELL_SIZE / 2 - 5;
                g.setColor(Color.BLUE);
                g.fillOval(droneX, droneY, 10, 10);
                g.drawString("Drone " + drone.getDroneID(), droneX - 10, droneY - 10);
            }
        }
    }

    /**
     * Updates the location of a drone and repaints the GUI.
     *
     * @param droneID the ID of the drone
     * @param x the new x-coordinate (in grid units, not pixels)
     * @param y the new y-coordinate (in grid units, not pixels)
     */
    public void updateDroneLocation(int droneID, int x, int y) {
        droneFleet.updateLocation(droneID, new Point(x, y));
        repaint();
    }

    /**
     * Processes incoming fire incident events. For each event, it updates the fireZones
     * map and, for DRONE_REQUEST events, starts the movement of assigned drones.
     *
     * @param events the list of fire incident events to process
     */
    public void receiveEvent(List<FireIncidentEvent> events) {
        for (FireIncidentEvent event : events) {
            String e = event.toString();
            if (processedEvents.contains(e)) {
                continue; // Skip already processed events.
            }
            processedEvents.add(e);

            String[] parts = e.split(",");
            System.out.println("Received event: " + Arrays.toString(parts));

            // Process FIRE_DETECTED events.
            if (parts[2].trim().equals("FIRE_DETECTED")) {
                int fireX = Integer.parseInt(parts[11].trim());
                int fireY = Integer.parseInt(parts[12].trim());

                // Map raw coordinates to grid coordinates.
                int gridX = (fireX / CELL_SIZE) % GRID_COLS;
                int gridY = (fireY / CELL_SIZE) % GRID_ROWS;
                Point fireLocation = new Point(gridX, gridY);

                // Mark the zone as on fire by incrementing the count.
                fireZones.put(new Point(gridX, gridY), fireZones.getOrDefault(new Point(gridX, gridY), 0) + 1);
                System.out.println("Fire detected at raw (" + fireX + "," + fireY + ") → grid (" + gridX + "," + gridY + ")");

                // If no drone is already assigned to this zone, assign an available drone.
                if (!assignedDrones.containsKey(fireLocation)) {
                    DroneData availableDrone = null;
                    for (DroneData drone : droneFleet.getDrones()) {
                        if (!droneTargets.containsKey(drone.getDroneID())) {
                            availableDrone = drone;
                            break;
                        }
                    }
                    if (availableDrone != null) {
                        assignedDrones.put(new Point(fireLocation), availableDrone.getDroneID());
                        droneTargets.put(availableDrone.getDroneID(), new Point(gridX, gridY));
                        availableDrone.setTargetLocation(new Point(gridX, gridY));
                    }
                }
                repaint();
            }
            // Process DRONE_REQUEST events.
            else if (parts[2].trim().equals("DRONE_REQUEST")) {
                waterNeeded = Integer.parseInt(parts[4].trim());
                hasFault = parts[5].trim().equals("NOZZLE_JAM") || parts[5].trim().equals("DOOR_STUCK");
                System.out.println("Drone request received. Available drones: " + droneFleet.getAvailableDrones());
                // Start movement threads for all drones that have an assigned target.
                for (DroneData drone : droneFleet.getDrones()) {
                    if (droneTargets.containsKey(drone.getDroneID())) {
                        new Thread(() -> moveDroneToTarget(drone.getDroneID())).start();
                    }
                }
            }
            // Delay between processing events.
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Simulates moving a drone toward its target zone, attempting to extinguish the fire,
     * handling water refill if necessary, and returning to the Drone Station.
     *
     * @param droneID the ID of the drone to move
     */
    private void moveDroneToTarget(int droneID) {
        DroneData drone = droneFleet.getADrone(droneID);
        Point target = droneTargets.get(droneID);
        if (target == null) {
            System.out.println("No target assigned for drone " + droneID);
            return;
        }
        System.out.println("Moving drone " + droneID + " to target " + target);
        Point currentLocation = drone.getLocation();

        // Move the drone to the target zone.
        while (!currentLocation.equals(target)) {
            int dx = Integer.compare(target.x, currentLocation.x);
            int dy = Integer.compare(target.y, currentLocation.y);
            currentLocation.translate(dx, dy);
            updateDroneLocation(droneID, currentLocation.x, currentLocation.y);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!hasFault) {
            logUpdate("Drone " + droneID + " reached target " + target);
            // Simulate dropping water until the fire's waterNeeded requirement is met.
            while (waterNeeded > drone.getRemainingPayload()) {
                waterNeeded = waterNeeded - drone.getRemainingPayload();
                logUpdate("Drone " + droneID + " dropped water; remaining water needed at " + target + ": " + waterNeeded);
                // Return to base to refill.
                Point base = new Point(0, 0);
                while (!currentLocation.equals(base)) {
                    int dx = Integer.compare(base.x, currentLocation.x);
                    int dy = Integer.compare(base.y, currentLocation.y);
                    currentLocation.translate(dx, dy);
                    updateDroneLocation(droneID, currentLocation.x, currentLocation.y);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                logUpdate("Drone " + droneID + " refilled water and is returning to target " + target);
                drone.refillWater();
                while (!currentLocation.equals(target)) {
                    int dx = Integer.compare(target.x, currentLocation.x);
                    int dy = Integer.compare(target.y, currentLocation.y);
                    currentLocation.translate(dx, dy);
                    updateDroneLocation(droneID, currentLocation.x, currentLocation.y);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            logUpdate("Drone " + droneID + " reached target " + target);
            logUpdate("Drone " + droneID + " has a hard fault; returning to base from " + target);
            // Return immediately to base if there's a fault.
            Point base = new Point(0, 0);
            while (!currentLocation.equals(base)) {
                int dx = Integer.compare(base.x, currentLocation.x);
                int dy = Integer.compare(base.y, currentLocation.y);
                currentLocation.translate(dx, dy);
                updateDroneLocation(droneID, currentLocation.x, currentLocation.y);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            logUpdate("Drone " + droneID + " refilled water at base.");
            while (!currentLocation.equals(target)) {
                int dx = Integer.compare(target.x, currentLocation.x);
                int dy = Integer.compare(target.y, currentLocation.y);
                currentLocation.translate(dx, dy);
                updateDroneLocation(droneID, currentLocation.x, currentLocation.y);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // Simulate extinguishing the fire.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Decrement the fire count in the zone.
        Integer count = fireZones.get(target);
        if (count != null) {
            if (count > 1) {
                fireZones.put(target, count - 1);
            } else {
                fireZones.remove(target);
            }
        }
        repaint();
        // Clear the assignment so the drone is free for future tasks.
        droneTargets.remove(droneID);

        // Return to the Drone Station at (0,0).
        Point base = new Point(0, 0);
        while (!currentLocation.equals(base)) {
            int dx = Integer.compare(base.x, currentLocation.x);
            int dy = Integer.compare(base.y, currentLocation.y);
            currentLocation.translate(dx, dy);
            updateDroneLocation(droneID, currentLocation.x, currentLocation.y);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logUpdate("Drone " + droneID + " returned to base.");
    }

    /**
     * The main entry point of the application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create the shared FireEventList and DroneFleet.
            FireEventList eventList = new FireEventList();
            DroneFleet sharedDroneFleet = new DroneFleet(eventList);
            FirefightingDroneGridGUI gui = new FirefightingDroneGridGUI(sharedDroneFleet);
            gui.setVisible(true);
            try {
                schedulerAddr = InetAddress.getByName(SCHEDULER_IP);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            // Add drones to the shared DroneFleet.
            int numberOfDrones = 7;
            for (int i = 1; i <= numberOfDrones; i++) {
                DroneData drone = new DroneData(i, new Point(0, 0), 15, schedulerAddr, 5000);
                sharedDroneFleet.addDrone(i, drone);
            }
            // Simulate receiving fire events periodically.
            new Thread(() -> {
                try {
                    while (true) {
                        FireIncidentSubSystem fireEvent = new FireIncidentSubSystem(
                                "src/main/resources/Sample_zone_file.csv",
                                "src/main/resources/Sample_event_file.csv"
                        );
                        List<FireIncidentEvent> events = fireEvent.loadFireIncidents("src/main/resources/Sample_event_file.csv");
                        if (events != null) {
                            Thread.sleep(2000);
                            gui.receiveEvent(events);
                        }
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
