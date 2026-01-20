import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MapTest {

    private static final int OBS = 1;

    // ----------------- Helpers -----------------

    private static void assertPixelEquals(Pixel2D p, int x, int y) {
        assertNotNull(p, "Pixel should not be null");
        assertEquals(x, p.getX(), "X mismatch");
        assertEquals(y, p.getY(), "Y mismatch");
    }

    private static void assertPathValid4Neighbors(Map map, Pixel2D[] path, int obsColor) {
        assertNotNull(path, "Path should not be null");
        assertTrue(path.length >= 1, "Path should have at least 1 node");

        for (int i = 0; i < path.length; i++) {
            Pixel2D p = path[i];
            assertNotNull(p, "Path contains null at index " + i);
            assertTrue(map.isInside(p), "Path pixel out of bounds at index " + i + ": (" + p.getX() + "," + p.getY() + ")");
            assertNotEquals(obsColor, map.getPixel(p), "Path goes through obstacle at index " + i);
        }

        for (int i = 1; i < path.length; i++) {
            int dx = Math.abs(path[i].getX() - path[i - 1].getX());
            int dy = Math.abs(path[i].getY() - path[i - 1].getY());

            int w = map.getWidth(), h = map.getHeight();
            boolean normal = (dx + dy) == 1;
            boolean wrapX = (dy == 0) && (dx == w - 1) && map.isCyclic();
            boolean wrapY = (dx == 0) && (dy == h - 1) && map.isCyclic();

            assertTrue(normal || wrapX || wrapY,
                    "Consecutive path pixels are not 4-neighbors at i=" + i +
                            " prev=(" + path[i - 1].getX() + "," + path[i - 1].getY() + ")" +
                            " cur=(" + path[i].getX() + "," + path[i].getY() + ")");
        }
    }

    // ----------------- Basic init/getters -----------------

    @Test
    void init_withValue_dimensionsAndFill() {
        Map m = new Map(4, 3, 7);
        assertEquals(4, m.getWidth());
        assertEquals(3, m.getHeight());
        assertEquals(7, m.getPixel(0, 0));
        assertEquals(7, m.getPixel(3, 2));
    }

    @Test
    void init_invalidDimensions_resultsEmptyMap() {
        Map m = new Map(0, 5, 1);
        assertEquals(0, m.getWidth());
        assertEquals(0, m.getHeight());
        assertArrayEquals(new int[0][0], m.getMap());
    }

    @Test
    void init_fromArray_copiesData_notAlias() {
        int[][] a = new int[][]{
                {1, 2},
                {3, 4}
        };
        Map m = new Map(a);

        assertEquals(2, m.getWidth());
        assertEquals(2, m.getHeight());
        assertEquals(1, m.getPixel(0, 0));
        assertEquals(4, m.getPixel(1, 1));

        a[0][0] = 99;
        assertEquals(1, m.getPixel(0, 0));

        int[][] copy = m.getMap();
        copy[1][1] = 88;
        assertEquals(4, m.getPixel(1, 1));
    }

    // ----------------- isInside -----------------

    @Test
    void isInside_basic() {
        Map m = new Map(3, 2, 0);
        assertTrue(m.isInside(new Index2D(0, 0)));
        assertTrue(m.isInside(new Index2D(2, 1)));

        assertFalse(m.isInside(new Index2D(-1, 0)));
        assertFalse(m.isInside(new Index2D(0, -1)));
        assertFalse(m.isInside(new Index2D(3, 0)));
        assertFalse(m.isInside(new Index2D(0, 2)));
        assertFalse(m.isInside(null));
    }

    // ----------------- fill (flood fill) -----------------

    @Test
    void fill_changesConnectedComponent_countCorrect_nonCyclic() {
        int[][] data = new int[][]{
                {0, 0, 1, 0},
                {0, 1, 1, 0},
                {0, 0, 0, 0}
        };
        Map m = new Map(data);
        m.setCyclic(false);

        int changed = m.fill(new Index2D(0, 0), 9);

        // Correct count is 9 (includes (0,1))
        assertEquals(9, changed);

        assertEquals(9, m.getPixel(0, 0));
        assertEquals(9, m.getPixel(2, 3));

        assertEquals(1, m.getPixel(0, 2));
        assertEquals(1, m.getPixel(1, 1));

        assertEquals(9, m.getPixel(0, 1));
    }

    @Test
    void fill_whenOldEqualsNew_returns0_andNoChange() {
        Map m = new Map(2, 2, 5);
        int changed = m.fill(new Index2D(1, 1), 5);
        assertEquals(0, changed);
        assertEquals(5, m.getPixel(0, 0));
        assertEquals(5, m.getPixel(1, 1));
    }

    @Test
    void fill_cyclicWrap_canReachAcrossEdges() {
        int[][] data = new int[][]{
                {0},
                {1},
                {0}
        };
        Map m = new Map(data);

        m.setCyclic(false);
        assertEquals(1, m.fill(new Index2D(0, 0), 7));
        assertEquals(7, m.getPixel(0, 0));
        assertEquals(0, m.getPixel(2, 0));

        m = new Map(data);
        m.setCyclic(true);
        int changed = m.fill(new Index2D(0, 0), 7);
        assertEquals(2, changed);
        assertEquals(7, m.getPixel(0, 0));
        assertEquals(7, m.getPixel(2, 0));
    }

    // ----------------- shortestPath -----------------

    @Test
    void shortestPath_sameStartEnd_returnsSinglePixel() {
        Map m = new Map(4, 4, 0);
        m.setCyclic(false);

        Pixel2D s = new Index2D(2, 3);
        Pixel2D[] path = m.shortestPath(s, s, OBS);

        assertNotNull(path);
        assertEquals(1, path.length);
        assertPixelEquals(path[0], 2, 3);
    }

    @Test
    void shortestPath_unreachable_returnsNull() {
        int[][] data = new int[][]{
                {0, 1, 0},
                {0, 1, 0},
                {0, 1, 0}
        };
        Map m = new Map(data);
        m.setCyclic(false);

        Pixel2D s = new Index2D(0, 0);
        Pixel2D t = new Index2D(2, 2);

        Pixel2D[] path = m.shortestPath(s, t, 1);
        assertNull(path, "Should be null when no path exists");
    }

    @Test
    void shortestPath_basicReachable_nonCyclic_validAndEndpoints() {
        int[][] data = new int[][]{
                {0, 0, 0, 0},
                {1, 1, 0, 1},
                {0, 0, 0, 0},
                {0, 1, 1, 0}
        };
        Map m = new Map(data);
        m.setCyclic(false);

        Pixel2D s = new Index2D(0, 0);
        Pixel2D t = new Index2D(3, 3);

        Pixel2D[] path = m.shortestPath(s, t, 1);
        assertPathValid4Neighbors(m, path, 1);
        assertEquals(s.getX(), path[0].getX());
        assertEquals(s.getY(), path[0].getY());
        assertEquals(t.getX(), path[path.length - 1].getX());
        assertEquals(t.getY(), path[path.length - 1].getY());

        int expectedNodes = 7;
        assertEquals(expectedNodes, path.length, "Shortest path length mismatch");
    }

    @Test
    void shortestPath_cyclicWrap_canShortenPath() {
        int[][] data = new int[][]{
                {0},{0},{0},{0},{0}
        };
        Map m = new Map(data);

        Pixel2D s = new Index2D(0, 0);
        Pixel2D t = new Index2D(4, 0);

        m.setCyclic(false);
        Pixel2D[] p1 = m.shortestPath(s, t, OBS);
        assertNotNull(p1);
        assertEquals(5, p1.length);

        m.setCyclic(true);
        Pixel2D[] p2 = m.shortestPath(s, t, OBS);
        assertNotNull(p2);
        assertEquals(2, p2.length);
        assertPathValid4Neighbors(m, p2, OBS);
    }

    @Test
    void shortestPath_startOrTargetIsObstacle_returnsNull() {
        Map m = new Map(3, 3, 0);
        m.setPixel(0, 0, OBS);
        Pixel2D[] path = m.shortestPath(new Index2D(0, 0), new Index2D(2, 2), OBS);
        assertNull(path);

        m = new Map(3, 3, 0);
        m.setPixel(2, 2, OBS);
        path = m.shortestPath(new Index2D(0, 0), new Index2D(2, 2), OBS);
        assertNull(path);
    }

    // ----------------- allDistance -----------------

    @Test
    void allDistance_openGrid_nonCyclic_correctDistances() {
        Map m = new Map(4, 3, 0);
        m.setCyclic(false);

        Map2D d = m.allDistance(new Index2D(0, 0), OBS);
        assertNotNull(d);

        assertEquals(0, d.getPixel(0, 0));
        assertEquals(1, d.getPixel(1, 0));
        assertEquals(1, d.getPixel(0, 1));
        assertEquals(5, d.getPixel(3, 2));
    }

    @Test
    void allDistance_obstaclesStayObs_unreachableIsMinus1() {
        int[][] data = new int[][]{
                {0, 1, 0},
                {0, 1, 0},
                {0, 1, 0}
        };
        Map m = new Map(data);
        m.setCyclic(false);

        Map2D d = m.allDistance(new Index2D(0, 0), 1);
        assertNotNull(d);

        assertEquals(1, d.getPixel(0, 1));
        assertEquals(1, d.getPixel(1, 1));
        assertEquals(1, d.getPixel(2, 1));

        assertEquals(-1, d.getPixel(2, 2));
        assertEquals(-1, d.getPixel(2, 0));

        assertEquals(0, d.getPixel(0, 0));
        assertEquals(1, d.getPixel(1, 0));
    }

    @Test
    void allDistance_startOnObstacle_returnsMapWithObs() {
        Map m = new Map(3, 3, 0);
        m.setPixel(1, 1, OBS);

        Map2D d = m.allDistance(new Index2D(1, 1), OBS);
        assertNotNull(d);

        assertEquals(OBS, d.getPixel(1, 1));
    }

    @Test
    void allDistance_cyclicWrap_distanceShorter() {
        int[][] data = new int[][]{
                {0},{0},{0},{0},{0}
        };
        Map m = new Map(data);
        m.setCyclic(true);

        Map2D d = m.allDistance(new Index2D(0, 0), OBS);
        assertNotNull(d);

        assertEquals(0, d.getPixel(0, 0));
        assertEquals(1, d.getPixel(4, 0));
        assertEquals(1, d.getPixel(1, 0));
        assertEquals(2, d.getPixel(2, 0));
    }
}
