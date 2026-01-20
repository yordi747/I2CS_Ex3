import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for Map (implements Map2D).
 *
 * Assumptions based on your code:
 * - Map stores data as int[w][h] where w = getWidth(), h = getHeight()
 * - fill(Pixel2D, new_v) flood-fills cells equal to the start's old value.
 * - shortestPath(p1, p2, obsColor) returns Pixel2D[] path inclusive, or null.
 * - allDistance(start, obsColor) returns a NEW Map2D with BFS distances, obstacles stay obsColor, unreachable = -1.
 * - cyclic behavior depends on setCyclic(boolean).
 *
 * NOTE: Requires your interfaces/classes: Map2D, Pixel2D, Index2D.
 */
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

            // In cyclic mode, wrap can make dx or dy large; we validate with map dimensions.
            // We'll allow either normal neighbor (dx+dy==1) OR wrap-neighbor (dx==w-1,dy==0) OR (dy==h-1,dx==0)
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

        // mutate original -> map should NOT change
        a[0][0] = 99;
        assertEquals(1, m.getPixel(0, 0));

        // mutate getMap() result -> map should NOT change
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
        // 0-region and separated by 1 walls
        int[][] data = new int[][]{
                {0, 0, 1, 0},
                {0, 1, 1, 0},
                {0, 0, 0, 0}
        };
        // Note: your map is [x][y], so above is width=3 height=4? Actually array is [3][4] here.
        // We'll keep it consistent: data.length = width, data[0].length = height.
        Map m = new Map(data);
        m.setCyclic(false);

        int changed = m.fill(new Index2D(0, 0), 9);
        // From (0,0) in this layout, reachable zeros are: (0,0),(1,0),(0,1),(0,2),(1,2),(2,2),(2,3),(1,3),(0,3)?? depends on walls.
        // Let's compute manually with the grid:
        // x=0 column: y0=0,y1=0,y2=1,y3=0
        // x=1 column: y0=0,y1=1,y2=1,y3=0
        // x=2 column: y0=0,y1=0,y2=0,y3=0
        // Starting at (0,0) old=0:
        // reachable (without crossing 1): (0,0),(1,0),(2,0),(2,1),(2,2),(2,3),(1,3),(0,3) => 8 cells
        assertEquals(8, changed);

        // verify some were changed
        assertEquals(9, m.getPixel(0, 0));
        assertEquals(9, m.getPixel(2, 3));

        // verify walls unchanged
        assertEquals(1, m.getPixel(0, 2));
        assertEquals(1, m.getPixel(1, 1));

        // verify a non-reachable 0? (0,1) is reachable through (0,0) actually, so it should be 9
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
        // Two 0 cells at opposite horizontal edges, separated if non-cyclic.
        // width=3 height=1
        int[][] data = new int[][]{
                {0},
                {1},
                {0}
        };
        Map m = new Map(data);

        m.setCyclic(false);
        assertEquals(1, m.fill(new Index2D(0, 0), 7)); // only itself
        assertEquals(7, m.getPixel(0, 0));
        assertEquals(0, m.getPixel(2, 0)); // unchanged

        // reset
        m = new Map(data);
        m.setCyclic(true);
        int changed = m.fill(new Index2D(0, 0), 7);
        // In cyclic mode, (0,0) neighbors include (2,0) via wrap => both zeros become 7
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
        // Blocked by obstacles
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
        // simple open grid with one obstacle column
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

        // Minimal length check (edges distance) for this specific map.
        // We expect a specific shortest path length in nodes.
        // If your BFS order changes, still shortest length should match.
        int expectedNodes = 7; // nodes count (edges distance 6)
        assertEquals(expectedNodes, path.length, "Shortest path length mismatch");
    }

    @Test
    void shortestPath_cyclicWrap_canShortenPath() {
        // 1x5 strip: start at (0,0), target at (4,0)
        // Without cyclic: distance 4 edges => 5 nodes
        // With cyclic: wrap neighbor => distance 1 edge => 2 nodes
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

        // In open grid, distance = Manhattan
        assertEquals(0, d.getPixel(0, 0));
        assertEquals(1, d.getPixel(1, 0));
        assertEquals(1, d.getPixel(0, 1));
        assertEquals(5, d.getPixel(3, 2)); // |3-0|+|2-0|=5
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

        // Obstacles remain obsColor in output by your implementation
        assertEquals(1, d.getPixel(0, 1));
        assertEquals(1, d.getPixel(1, 1));
        assertEquals(1, d.getPixel(2, 1));

        // Cells on the other side of wall are unreachable -> -1
        assertEquals(-1, d.getPixel(2, 2));
        assertEquals(-1, d.getPixel(2, 0));

        // Reachable side distances
        assertEquals(0, d.getPixel(0, 0));
        assertEquals(1, d.getPixel(1, 0));
        assertEquals(2, d.getPixel(2, 0) == -1 ? -1 : d.getPixel(2, 0)); // keep consistent: here it's unreachable so -1 already asserted
    }

    @Test
    void allDistance_startOnObstacle_returnsMapWithObs() {
        Map m = new Map(3, 3, 0);
        m.setPixel(1, 1, OBS);

        Map2D d = m.allDistance(new Index2D(1, 1), OBS);
        assertNotNull(d);

        // Your code: if start is obstacle -> return new Map(dist) where dist[start] = obsColor (not 0)
        assertEquals(OBS, d.getPixel(1, 1));
    }

    @Test
    void allDistance_cyclicWrap_distanceShorter() {
        // 5x1 strip, cyclic makes (0,0) neighbor of (4,0)
        int[][] data = new int[][]{
                {0},{0},{0},{0},{0}
        };
        Map m = new Map(data);
        m.setCyclic(true);

        Map2D d = m.allDistance(new Index2D(0, 0), OBS);
        assertNotNull(d);

        assertEquals(0, d.getPixel(0, 0));
        assertEquals(1, d.getPixel(4, 0)); // wrap
        assertEquals(1, d.getPixel(1, 0));
        assertEquals(2, d.getPixel(2, 0)); // can be reached via 1 or 4 then...
    }
}
