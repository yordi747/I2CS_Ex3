package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Minimal server-side game state:
 * - board (WALL/EMPTY/DOT/POWER) loaded from LevelLoader
 * - pacman position + score
 * - ghosts positions (simple)
 * - power mode timer
 *
 * Coordinates: board[x][y] (x first) just like your Map.
 * y=0 is top row (same as PNG reading).
 */
public class MyPacmanGame {

    // Cell codes (same as LevelLoader)
    public static final int EMPTY = LevelLoader.EMPTY;
    public static final int WALL  = LevelLoader.WALL;
    public static final int DOT   = LevelLoader.DOT;
    public static final int POWER = LevelLoader.POWER;

    // Simple directions (match your Ex3Algo style: UP, LEFT, DOWN, RIGHT)
    public static final int UP = 0, LEFT = 1, DOWN = 2, RIGHT = 3;

    // Game state
    private final int[][] board;      // changes: DOT/POWER get eaten -> EMPTY
    private final int w, h;

    private int pacX, pacY;
    private int score;

    private final List<Ghost> ghosts = new ArrayList<>();
    private int powerTicks;           // >0 => ghosts are eatable

    private final Random rnd = new Random(1);

    public MyPacmanGame(int[][] loadedBoard) {
        if (loadedBoard == null || loadedBoard.length == 0 || loadedBoard[0].length == 0) {
            throw new IllegalArgumentException("board is empty");
        }
        this.w = loadedBoard.length;
        this.h = loadedBoard[0].length;
        this.board = deepCopy(loadedBoard);

        // pick a start cell for pacman: first EMPTY/DOT/POWER found
        int[] p = findFirstWalkable();
        this.pacX = p[0];
        this.pacY = p[1];
        this.score = 0;
        this.powerTicks = 0;

        // spawn a few ghosts on walkable cells (away from pacman if possible)
        spawnGhosts(3);
    }

    /* -------------------- Public getters -------------------- */

    public int[][] getBoard() {
        return deepCopy(board);
    }

    public int getWidth() { return w; }
    public int getHeight() { return h; }

    public int getPacX() { return pacX; }
    public int getPacY() { return pacY; }

    public int getScore() { return score; }
    public int getPowerTicks() { return powerTicks; }

    public List<Ghost> getGhosts() {
        return ghosts; // return direct list (ok for internal), or copy if you prefer
    }

    public int remainingDots() {
        int cnt = 0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (board[x][y] == DOT || board[x][y] == POWER) cnt++;
            }
        }
        return cnt;
    }

    /* -------------------- Tick update -------------------- */

    /**
     * One game step:
     * 1) move pacman by dir if possible
     * 2) eat DOT/POWER
     * 3) move ghosts (simple random chase)
     * 4) resolve collisions
     * 5) decay power timer
     */
    public StepResult step(int pacDir) {
        // 1) move pacman
        movePacman(pacDir);

        // 2) eat
        eatAtPacman();

        // 3) move ghosts
        moveGhosts();

        // 4) collisions
        boolean died = resolveCollisions();

        // 5) decay power
        if (powerTicks > 0) powerTicks--;

        // win/lose
        if (died) return StepResult.LOSE;
        if (remainingDots() == 0) return StepResult.WIN;
        return StepResult.CONTINUE;
    }

    public enum StepResult { CONTINUE, WIN, LOSE }

    /* -------------------- Movement -------------------- */

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

    private void eatAtPacman() {
        int cell = board[pacX][pacY];
        if (cell == DOT) {
            score += 10;
            board[pacX][pacY] = EMPTY;
        } else if (cell == POWER) {
            score += 50;
            board[pacX][pacY] = EMPTY;
            powerTicks = 80; // ~80 ticks power (tune)
        }
    }

    private void moveGhosts() {
        for (Ghost g : ghosts) {
            int dir = chooseGhostDir(g);
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
     * Simple ghost AI:
     * - If not eatable: try move towards pacman (greedy)
     * - If eatable: try move away (greedy)
     * Falls back to random valid move.
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
                // chase => minimize dist
                if (dist < bestScore) { bestScore = dist; bestDir = d; }
            } else {
                // run => maximize dist
                if (dist > bestScore) { bestScore = dist; bestDir = d; }
            }
        }

        if (bestDir != -1) return bestDir;

        // random fallback
        return dirs[rnd.nextInt(dirs.length)];
    }

    private boolean resolveCollisions() {
        for (Ghost g : ghosts) {
            if (g.x == pacX && g.y == pacY) {
                if (powerTicks > 0) {
                    // eat ghost
                    score += 200;
                    respawnGhost(g);
                } else {
                    return true; // pacman dies
                }
            }
        }
        return false;
    }

    private void respawnGhost(Ghost g) {
        int[] p = findWalkableFarFromPacman();
        g.x = p[0];
        g.y = p[1];
        g.lastDir = -1;
    }

    /* -------------------- Spawn helpers -------------------- */

    private void spawnGhosts(int k) {
        for (int i = 0; i < k; i++) {
            int[] p = findWalkableFarFromPacman();
            ghosts.add(new Ghost(p[0], p[1]));
        }
    }

    private int[] findFirstWalkable() {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isWalkable(x, y)) return new int[]{x, y};
            }
        }
        return new int[]{0, 0};
    }

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

    private boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return false;
        return board[x][y] != WALL;
    }

    private static int manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private static int[][] deepCopy(int[][] a) {
        int[][] b = new int[a.length][a[0].length];
        for (int i = 0; i < a.length; i++) {
            System.arraycopy(a[i], 0, b[i], 0, a[i].length);
        }
        return b;
    }

    /* -------------------- Ghost inner class -------------------- */

    public static class Ghost {
        public int x, y;
        public int lastDir = -1;

        public Ghost(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}

