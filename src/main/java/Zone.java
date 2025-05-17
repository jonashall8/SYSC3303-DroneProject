import java.awt.*;

/**
 * The {@code Zone} class represents a rectangular area with a start point,
 * an end point and a calculated center point. Each zone has a unique ID.
 */
public class Zone {
    private int id;
    private Point start;
    private Point end;
    private Point center;

    /**
     * Constructs a {@code Zone} with specified coordinates.
     *
     * @param id     The unique identifier for the zone.
     * @param startX The x-coordinate of the starting point.
     * @param startY The y-coordinate of the starting point.
     * @param endX   The x-coordinate of the ending point.
     * @param endY   The y-coordinate of the ending point.
     */
    public Zone(int id, int startX, int startY, int endX, int endY) {
        this.id = id;
        this.start = new Point(startX, startY);
        this.end = new Point(endX, endY);
        calculateCenter();
    }

    /**
     * Retrieves the unique ID of the zone.
     *
     * @return The zone ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the starting point of the zone.
     *
     * @return The start point as a {@code Point}.
     */
    public Point getStart() {
        return start;
    }

    /**
     * Retrieves the ending point of the zone.
     *
     * @return The end point as a {@code Point}.
     */
    public Point getEnd() {
        return end;
    }

    /**
     * Retrieves the center point of the zone.
     *
     * @return The center point as a {@code Point}.
     */
    public Point getCenter() {
        return center;
    }

    /**
     * Calculates the center point of the zone using the midpoint formula.
     */
    private void calculateCenter() {
        this.center = new Point((start.x + end.x) / 2, (start.y + end.y) / 2);
        System.out.println("This is the center of the zone: " + this.center);
    }

    @Override
    public String toString() {
        return id + "," + start.x + "," + start.y + "," + end.x + "," + end.y;
    }
}
