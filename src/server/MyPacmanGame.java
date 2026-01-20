package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MyPacmanGame {

    public static final int EMPTY = LevelLoaderEx3.EMPTY;
    public static final int WALL  = LevelLoaderEx3.WALL;
    public static final int DOT   = LevelLoaderEx3.DOT;
    public static final int POWER = LevelLoaderEx3.POWER;

    public static final int UP = 0, LEFT = 1, DOWN = 2, RIGHT = 3;

    private final int[][] board;
    private final int w, h;

    private int pacX, pacY;
    private int score;

    private final List<Ghost> ghosts = new ArrayList<>();
    private int powerTicks;

    private final Random rnd = new Random(1);

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
        return ghosts;
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

    public enum StepResult { CONTINUE, WIN, LOSE }

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
            powerTicks = 80;
        }
    }

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

        if (bestDir != -1) return bestDir;

        int[] legal = legalDirsFrom(g.x, g.y);
        if (legal.length == 0) return -1;
        return legal[rnd.nextInt(legal.length)];
    }

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

    private void respawnGhost(Ghost g) {
        int[] p = findWalkableFarFromPacman();
        g.x = p[0];
        g.y = p[1];
        g.lastDir = -1;
    }

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

    public static class Ghost {
        public int x, y;
        public int lastDir = -1;

        public Ghost(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
