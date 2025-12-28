import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * This class represents a 2D map as a "screen" or a raster matrix or maze over integers.
 * @author boaz.benmoshe
 *
 */
public class Map implements Map2D {
    private int[][] _map;
    private boolean _cyclicFlag = true;

    /**
     * Constructs a w*h 2D raster map with an init value v.
     */
    public Map(int w, int h, int v) { init(w, h, v); }

    /**
     * Constructs a square map (size*size).
     */
    public Map(int size) { this(size, size, 0); }

    /**
     * Constructs a map from a given 2D array.
     */
    public Map(int[][] data) {
        init(data);
    }

    @Override
    public void init(int w, int h, int v) {
        if (w <= 0 || h <= 0) {
            _map = new int[0][0];
            return;
        }
        _map = new int[w][h];
        for (int x = 0; x < w; x++) {
            Arrays.fill(_map[x], v);
        }
    }

    @Override
    public void init(int[][] arr) {
        if (arr == null || arr.length == 0 || arr[0] == null || arr[0].length == 0) {
            _map = new int[0][0];
            return;
        }
        int w = arr.length;
        int h = arr[0].length;
        _map = new int[w][h];
        for (int x = 0; x < w; x++) {
            if (arr[x] == null || arr[x].length != h) {
                throw new IllegalArgumentException("init(int[][]): jagged or null row at x=" + x);
            }
            System.arraycopy(arr[x], 0, _map[x], 0, h);
        }
    }

    @Override
    public int[][] getMap() {
        if (_map == null || _map.length == 0) return new int[0][0];
        int w = _map.length;
        int h = _map[0].length;
        int[][] ans = new int[w][h];
        for (int x = 0; x < w; x++) {
            System.arraycopy(_map[x], 0, ans[x], 0, h);
        }
        return ans;
    }

    @Override
    public int getWidth() {
        return (_map == null) ? 0 : _map.length;
    }

    @Override
    public int getHeight() {
        return (_map == null || _map.length == 0) ? 0 : _map[0].length;
    }

    @Override
    public int getPixel(int x, int y) {
        return _map[x][y];
    }

    @Override
    public int getPixel(Pixel2D p) {
        return this.getPixel(p.getX(), p.getY());
    }

    @Override
    public void setPixel(int x, int y, int v) {
        _map[x][y] = v;
    }

    @Override
    public void setPixel(Pixel2D p, int v) {
        setPixel(p.getX(), p.getY(), v);
    }

    @Override
    /**
     * Fills this map with the new color (new_v) starting from p.
     * https://en.wikipedia.org/wiki/Flood_fill
     */
    public int fill(Pixel2D xy, int new_v) {
        if (xy == null) return 0;
        if (!isInside(xy)) return 0;

        int sx = xy.getX();
        int sy = xy.getY();
        int old = getPixel(sx, sy);
        if (old == new_v) return 0;

        int w = getWidth(), h = getHeight();
        boolean[][] vis = new boolean[w][h];
        ArrayDeque<Index2D> q = new ArrayDeque<>();
        q.add(new Index2D(sx, sy));
        vis[sx][sy] = true;

        int count = 0;

        while (!q.isEmpty()) {
            Index2D p = q.poll();
            int x = p.getX();
            int y = p.getY();

            if (_map[x][y] != old) continue;

            _map[x][y] = new_v;
            count++;

            // 4-neighbors
            int[][] dirs = {{0,-1},{-1,0},{0,1},{1,0}};
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];

                if (_cyclicFlag) {
                    nx = mod(nx, w);
                    ny = mod(ny, h);
                }

                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (vis[nx][ny]) continue;
                if (_map[nx][ny] != old) continue;

                vis[nx][ny] = true;
                q.add(new Index2D(nx, ny));
            }
        }

        return count;
    }

    @Override
    /**
     * BFS shortest path computation (4-neighbors).
     * obsColor = obstacle value (cannot pass through).
     * Returns an array of pixels from p1 to p2 (inclusive), or null if no path.
     */
    public Pixel2D[] shortestPath(Pixel2D p1, Pixel2D p2, int obsColor) {
        if (p1 == null || p2 == null) return null;
        if (!isInside(p1) || !isInside(p2)) return null;

        int w = getWidth(), h = getHeight();
        int sx = p1.getX(), sy = p1.getY();
        int tx = p2.getX(), ty = p2.getY();

        if (_map[sx][sy] == obsColor || _map[tx][ty] == obsColor) return null;
        if (sx == tx && sy == ty) return new Pixel2D[]{ new Index2D(sx, sy) };

        boolean[][] vis = new boolean[w][h];
        Index2D[][] prev = new Index2D[w][h];

        ArrayDeque<Index2D> q = new ArrayDeque<>();
        q.add(new Index2D(sx, sy));
        vis[sx][sy] = true;

        while (!q.isEmpty()) {
            Index2D cur = q.poll();
            int x = cur.getX();
            int y = cur.getY();

            int[][] dirs = {{0,-1},{-1,0},{0,1},{1,0}};
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];

                if (_cyclicFlag) {
                    nx = mod(nx, w);
                    ny = mod(ny, h);
                }

                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (vis[nx][ny]) continue;
                if (_map[nx][ny] == obsColor) continue;

                vis[nx][ny] = true;
                prev[nx][ny] = new Index2D(x, y);

                if (nx == tx && ny == ty) {
                    return buildPath(prev, sx, sy, tx, ty);
                }

                q.add(new Index2D(nx, ny));
            }
        }

        return null;
    }

    private Pixel2D[] buildPath(Index2D[][] prev, int sx, int sy, int tx, int ty) {
        // reconstruct backwards
        ArrayDeque<Pixel2D> stack = new ArrayDeque<>();
        int cx = tx, cy = ty;
        stack.push(new Index2D(cx, cy));

        while (!(cx == sx && cy == sy)) {
            Index2D p = prev[cx][cy];
            if (p == null) return null; // safety
            cx = p.getX();
            cy = p.getY();
            stack.push(new Index2D(cx, cy));
        }

        Pixel2D[] path = new Pixel2D[stack.size()];
        int i = 0;
        while (!stack.isEmpty()) {
            path[i++] = stack.pop();
        }
        return path;
    }

    @Override
    public boolean isInside(Pixel2D p) {
        if (p == null) return false;
        int x = p.getX(), y = p.getY();
        return x >= 0 && y >= 0 && x < getWidth() && y < getHeight();
    }

    @Override
    public boolean isCyclic() {
        return _cyclicFlag;
    }

    @Override
    public void setCyclic(boolean cy) {
        _cyclicFlag = cy;
    }

    @Override
    /**
     * Returns a NEW Map2D where each cell contains the BFS distance from start.
     * Obstacles (obsColor) remain as obsColor.
     * Unreachable cells get -1.
     * start gets 0.
     */
    public Map2D allDistance(Pixel2D start, int obsColor) {
        if (start == null || !isInside(start)) return null;

        int w = getWidth(), h = getHeight();
        int[][] dist = new int[w][h];
        for (int x = 0; x < w; x++) Arrays.fill(dist[x], -1);

        // keep obstacles as obsColor in the output (as requested in many versions)
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (_map[x][y] == obsColor) dist[x][y] = obsColor;
            }
        }

        int sx = start.getX(), sy = start.getY();
        if (_map[sx][sy] == obsColor) return new Map(dist);

        ArrayDeque<Index2D> q = new ArrayDeque<>();
        q.add(new Index2D(sx, sy));
        dist[sx][sy] = 0;

        while (!q.isEmpty()) {
            Index2D cur = q.poll();
            int x = cur.getX(), y = cur.getY();
            int cd = dist[x][y];

            int[][] dirs = {{0,-1},{-1,0},{0,1},{1,0}};
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];

                if (_cyclicFlag) {
                    nx = mod(nx, w);
                    ny = mod(ny, h);
                }

                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (_map[nx][ny] == obsColor) continue;
                if (dist[nx][ny] != -1 && dist[nx][ny] != obsColor) continue;

                dist[nx][ny] = cd + 1;
                q.add(new Index2D(nx, ny));
            }
        }

        return new Map(dist);
    }

    private static int mod(int a, int m) {
        int r = a % m;
        return (r < 0) ? r + m : r;
    }
}
