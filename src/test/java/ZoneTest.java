import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class ZoneTest {

    @Test
    void testZoneInitialization() {
        Zone zone = new Zone(1, 0, 0, 10, 10);

        assertEquals(1, zone.getId(), "Zone ID should be initialized correctly");
        assertEquals(new Point(0, 0), zone.getStart(), "Start point should be (0,0)");
        assertEquals(new Point(10, 10), zone.getEnd(), "End point should be (10,10)");
        assertEquals(new Point(5, 5), zone.getCenter(), "Center should be correctly calculated");
    }

    @Test
    void testNegativeCoordinates() {
        Zone zone = new Zone(2, -5, -5, 5, 5);

        assertEquals(new Point(-5, -5), zone.getStart(), "Start point should be (-5,-5)");
        assertEquals(new Point(5, 5), zone.getEnd(), "End point should be (5,5)");
        assertEquals(new Point(0, 0), zone.getCenter(), "Center should be correctly calculated");
    }

    @Test
    void testHorizontalZone() {
        Zone zone = new Zone(3, 2, 4, 10, 4);

        assertEquals(new Point(6, 4), zone.getCenter(), "Center should be in the middle horizontally");
    }

    @Test
    void testVerticalZone() {
        Zone zone = new Zone(4, 5, 1, 5, 9);

        assertEquals(new Point(5, 5), zone.getCenter(), "Center should be in the middle vertically");
    }

}
