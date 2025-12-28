import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MapTest {

    @Test
    void testInitWHV() {
        Map m = new Map(4, 3, 7);
        assertEquals(4, m.getWidth());
        assertEquals(3, m.getHeight());
        for (int x = 0; x < m.getWidth(); x++) {
            for (int y = 0; y < m.getHeight(); y++) {
                assertEquals(7, m.getPixel(x, y));
            }
        }
    }

    @Test
    void testInitArrayDeepCopy() {
        int[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        };
        Map m = new Map(a);

        // original array change should not affect the map
        a[0][0] = 999;
        assertEquals(1, m.getPixel(0, 0));

        // getMap must return deep copy
        int[][] copy = m.getMap();
        copy[1][2] = 777;
        assertEquals(6, m.getPixel(1, 2));
    }

    @Test
    void testGetSetPixel() {
        Map m = new Map(3, 3, 0);

        m.setPixel(1, 2, 9);
        assertEquals(9, m.getPixel(1, 2));

        Pixel2D p = new Index2D(0, 0);
        m.setPixel(p, 5);
        assertEquals(5, m.getPixel(0, 0));
        assertEquals(5, m.getPixel(p));
    }

    @Test
    void testIsInside() {
        Map m = new Map(2, 2, 0);
        assertTrue(m.isInside(new Index2D(0, 0)));
        assertTrue(m.isInside(new Index2D(1, 1)));
        assertFalse(m.isInside(new Index2D(-1, 0)));
        assertFalse(m.isInside(new Index2D(0, -1)));
        assertFalse(m.isInside(new Index2D(2, 0)));
        assertFalse(m.isInside(new Index2D(0, 2)));
    }

    @Test
    void testCyclicFlag() {
        Map m = new Map(2, 2, 0);
        m.setCyclic(true);
        assertTrue(m.isCyclic());
        m.setCyclic(false);
        assertFalse(m.isCyclic());
    }

    @Test
    void testFillNonCyclic() {
        // 1 1 0
        // 1 0 0
        // 0 0 2
        int[][] a = {
                {1, 1, 0},
                {1, 0, 0},
                {0, 0, 2}
        };
        Map m = new Map(a);
        m.setCyclic(false);

        int filled = m.fill(new Index2D(0, 0), 9);
        assertEquals(3, filled); // three connected '1' cells
        assertEquals(9, m.getPixel(0, 0));
        assertEquals(9, m.getPixel(0, 1));
        assertEquals(9, m.getPixel(1, 0));

        assertEquals(2, m.getPixel(2, 2)); // not changed
    }

    @Test
    void testShortestPathBasic() {
        int O = 1;
        int[][] a = {
                {0, 0, 0},
                {O, O, 0},
                {0, 0, 0}
        };
        Map m = new Map(a);
        m.setCyclic(false);

        Pixel2D start = new Index2D(0, 0);
        Pixel2D end = new Index2D(2, 2);

        Pixel2D[] path = m.shortestPath(start, end, O);
        assertNotNull(path);
        assertEquals(start, path[0]);
        assertEquals(end, path[path.length - 1]);

        // validate neighbor steps and no obstacles
        for (int i = 0; i < path.length; i++) {
            Pixel2D p = path[i];
            assertNotEquals(O, m.getPixel(p));
            if (i > 0) {
                Pixel2D prev = path[i - 1];
                int dx = Math.abs(p.getX() - prev.getX());
                int dy = Math.abs(p.getY() - prev.getY());
                assertEquals(1, dx + dy, "Each step must be 4-neighbors");
            }
        }
    }

    @Test
    void testShortestPathNoPath() {
        int O = 1;
        int[][] a = {
                {0, O, 0},
                {O, O, O},
                {0, O, 0}
        };
        Map m = new Map(a);
        m.setCyclic(false);

        assertNull(m.shortestPath(new Index2D(0, 0), new Index2D(2, 2), O));
    }

    @Test
    void testAllDistanceBasic() {
        int O = 1;
        int[][] a = {
                {0, 0, 0},
                {0, O, 0},
                {0, 0, 0}
        };
        Map m = new Map(a);
        m.setCyclic(false);

        Map2D dist = m.allDistance(new Index2D(0, 0), O);
        assertNotNull(dist);

        // start must be 0
        assertEquals(0, dist.getPixel(0, 0));

        // obstacle handling: some solutions keep O, others use -1. accept both.
        int obsVal = dist.getPixel(1, 1);
        assertTrue(obsVal == O || obsVal == -1, "Obstacle cell should be O or -1 depending on implementation");

        // reachable distances (must be >=0)
        assertTrue(dist.getPixel(1, 0) >= 0);
        assertTrue(dist.getPixel(2, 0) >= 0);
        assertTrue(dist.getPixel(0, 2) >= 0);
    }
}
