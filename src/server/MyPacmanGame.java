package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MyPacmanGame is a small server-side simulation of a Pac-Man board.
 *
 * It keeps:
 * - A 2D board (walls, dots, power pellets, empty)
 * - Pac-Man position + score
 * - Ghost positions (simple AI)
 * - A power mode timer (when Pac-Man can eat ghosts)
 *
 * Coordinate convention:
 * board[x][y] where (0,0) is top-left (same as image loading).
 *
 * This class is useful for testing your algorithm without the full GUI engine.
 */
public class MyPacmanGame {

    /** Cell type constants (same codes as LevelLoaderEx3) */
    public static final int EMPTY = LevelLoaderEx3.EMPTY;
    public static final int WALL  = LevelLoaderEx3.WALL;
    public static final int DOT   = LevelLoaderEx3.DOT;
    public static final int POWER = LevelLoaderEx3.POWER;

    /** Direction constants (match common Pac-Man direction order) */
    public static final int UP = 0, LEFT = 1, DOWN = 2, RIGHT = 3;

    /** Board state: board[x][y] */
    private final int[][] board;
    private final int w, h;

    /** Pac-Man state */
    private int pacX, pacY;
    private int score;

    /** Ghosts list + power mode timer */
    private final List<Ghost> ghosts = new ArrayList<>();
    private int powerTicks;

    /** Random generator (fixed seed for repeatable behavior) */
    private final Random rnd = new Random(1);

    /**
     * Creates a new game from a loaded board.
     * Pac-Man is placed at the first walkable cell.
     * A few ghosts are spawned far away from Pac-Man.
     *
     * @param loadedBoard board matrix as returned by LevelLoaderEx3
     */
    public MyPacmanGame(int[][] loadedBoard) {
        if (loadedBoard == null || loadedBoard.length == 0 || loadedBoard[0].length == 0) {
            throw new IllegalArgumentException("board is empty");
        }
        this.w = loadedBoard.length;
        this.h = loadedBoard[0].length;
        this.board = deepCopy(loadedBoard);

        int[] p = findFirstWalkable();
        this.pacX = p[0];
        this.pacY = p[1];
        this.score = 0;
        this.powerTicks = 0;

        spawnGhosts(3);
    }

    /**
     * @return a defensive copy of the current board state
     */
    public int[][] getBoard() {
        return deepCopy(board);
    }

    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public int getPacX() { return pacX; }
    public int getPacY() { return pacY; }

    public int getScore() { return score; }
    public int getPowerTicks() { return powerTicks; }

    /**
     * @return the internal ghost list (not copied)
     * Note: caller should not modify it unless you want that behavior.
     */
    public List<Ghost> getGhosts() {
        return ghosts;
    }

    /**
     * Counts how many dots/power pellets are still on the board.
     *
     * @return number of remaining DOT/POWER cells
     */
    public int remainingDots() {
        int cnt = 0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (board[x][y] == DOT || board[x][y] == POWER) cnt++;
            }
        }
        return cnt;
    }

    /**
     * Performs one "tick" of the game:
     * 1) move Pac-Man
     * 2) eat dot/power if standing on it
     * 3) move ghosts
     * 4) check collisions
     * 5) decrease power timer
     *
     * @param pacDir direction for Pac-Man (UP/LEFT/DOWN/RIGHT)
     * @return current game result (continue / win / lose)
     */
    public StepResult step(int pacDir) {
        movePacman(pacDir);
        eatAtPacman();
        moveGhosts();
        boolean died = resolveCollisions();
        if (powerTicks > 0) powerTicks--;
        if (died) return StepResult.LOSE;
        if (remainingDots() == 0) return StepResult.WIN;
        return StepResult.CONTINUE;
    }

    /** Possible outcomes after a step */
    public enum StepResult { CONTINUE, WIN, LOSE }

    /**
     * Moves Pac-Man one cell in the given direction if the target is walkable.
     */
    private void movePacman(int dir) {
        int nx = pacX, ny = pacY;
        if (dir == UP) ny--;
        else if (dir == DOWN) ny++;
        else if (dir == LEFT) nx--;
        else if (dir == RIGHT) nx++;

        if (isWalkable(nx, ny)) {
            pacX = nx; pacY = ny;
        }
    }

    /**
     * Handles eating logic:
     * - DOT gives +10 and becomes EMPTY
     * - POWER gives +50, becomes EMPTY, and starts power mode
     */
    private void eatAtPacman() {
        int cell = board[pacX][pacY];
        if (cell == DOT) {
            score += 10;
            board[pacX][pacY] = EMPTY;
        } else if (cell == POWER) {
            score += 50;
            board[pacX][pacY] = EMPTY;
            powerTicks = 80;
        }
    }

    /**
     * Moves each ghost one step using a simple rule:
     * - If Pac-Man is NOT powered: ghosts try to get closer
     * - If Pac-Man IS powered: ghosts try to run away
     */
    private void moveGhosts() {
        for (Ghost g : ghosts) {
            int dir = chooseGhostDir(g);
            if (dir == -1) continue;

            int nx = g.x, ny = g.y;
            if (dir == UP) ny--;
            else if (dir == DOWN) ny++;
            else if (dir == LEFT) nx--;
            else if (dir == RIGHT) nx++;

            if (isWalkable(nx, ny)) {
                g.x = nx; g.y = ny;
                g.lastDir = dir;
            }
        }
    }

    /**
     * Picks the best direction for a ghost using Manhattan distance to Pac-Man.
     * When Pac-Man is powered, it picks the move that increases distance.
     *
     * @return direction constant or -1 if no move exists
     */
    private int chooseGhostDir(Ghost g) {
        boolean eatable = (powerTicks > 0);

        int bestDir = -1;
        int bestScore = eatable ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        int[] dirs = {UP, LEFT, DOWN, RIGHT};

        for (int d : dirs) {
            int nx = g.x, ny = g.y;
            if (d == UP) ny--;
            else if (d == DOWN) ny++;
            else if (d == LEFT) nx--;
            else if (d == RIGHT) nx++;

            if (!isWalkable(nx, ny)) continue;

            int dist = manhattan(nx, ny, pacX, pacY);
            if (!eatable) {
                if (dist < bestScore) { bestScore = dist; bestDir = d; }
            } else {
                if (dist > bestScore) { bestScore = dist; bestDir = d; }
            }
        }

        // Fallback: if somehow no best dir, pick any legal move randomly
        if (bestDir != -1) return bestDir;

        int[] legal = legalDirsFrom(g.x, g.y);
        if (legal.length == 0) return -1;
        return legal[rnd.nextInt(legal.length)];
    }

    /**
     * @return array of directions that are legal (walkable) from (x,y)
     */
    private int[] legalDirsFrom(int x, int y) {
        int[] tmp = new int[4];
        int c = 0;

        if (isWalkable(x, y - 1)) tmp[c++] = UP;
        if (isWalkable(x - 1, y)) tmp[c++] = LEFT;
        if (isWalkable(x, y + 1)) tmp[c++] = DOWN;
        if (isWalkable(x + 1, y)) tmp[c++] = RIGHT;

        int[] out = new int[c];
        System.arraycopy(tmp, 0, out, 0, c);
        return out;
    }

    /**
     * Handles collisions between Pac-Man and ghosts:
     * - If Pac-Man is powered: ghost is eaten and respawned, +200 score
     * - Otherwise: Pac-Man dies (lose)
     *
     * @return true if Pac-Man died
     */
    private boolean resolveCollisions() {
        for (Ghost g : ghosts) {
            if (g.x == pacX && g.y == pacY) {
                if (powerTicks > 0) {
                    score += 200;
                    respawnGhost(g);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Respawns a ghost on a walkable cell far from Pac-Man.
     */
    private void respawnGhost(Ghost g) {
        int[] p = findWalkableFarFromPacman();
        g.x = p[0];
        g.y = p[1];
        g.lastDir = -1;
    }

    /**
     * Spawns k ghosts on walkable cells far from Pac-Man.
     */
    private void spawnGhosts(int k) {
        for (int i = 0; i < k; i++) {
            int[] p = findWalkableFarFromPacman();
            ghosts.add(new Ghost(p[0], p[1]));
        }
    }

    /**
     * Finds the first walkable cell in scan order (top-left to bottom-right).
     * Used to place Pac-Man at the start.
     */
    private int[] findFirstWalkable() {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isWalkable(x, y)) return new int[]{x, y};
            }
        }
        return new int[]{0, 0};
    }

    /**
     * Finds a random walkable cell that is (approximately) far from Pac-Man.
     * It samples a number of random cells and keeps the best one.
     */
    private int[] findWalkableFarFromPacman() {
        int bestX = pacX, bestY = pacY, bestD = -1;
        for (int t = 0; t < 500; t++) {
            int x = rnd.nextInt(w);
            int y = rnd.nextInt(h);
            if (!isWalkable(x, y)) continue;
            int d = manhattan(x, y, pacX, pacY);
            if (d > bestD) { bestD = d; bestX = x; bestY = y; }
        }
        return new int[]{bestX, bestY};
    }

    /**
     * Checks if a cell is inside bounds and not a wall.
     */
    private boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return false;
        return board[x][y] != WALL;
    }

    /**
     * Manhattan distance (grid distance) between two points.
     */
    private static int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    /**
     * Creates a deep copy of a 2D int array.
     * Used to avoid exposing the internal board directly.
     */
    private static int[][] deepCopy(int[][] a) {
        int[][] b = new int[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            System.arraycopy(a[i], 0, b[i], 0, a[i].length);
        }
        return b;
    }

    /**
     * Simple ghost data structure:
     * - (x,y) position on the board
     * - lastDir can be used for smarter movement (optional)
     */
    public static class Ghost {
        public int x, y;
        public int lastDir = -1;

        public Ghost(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
