import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ex3 - PacMan algorithm (client side).
 *
 * Strategy (high level):
 * 1) If there are eatable ghosts -> chase nearest eatable ghost safely.
 * 2) Otherwise eat nearest safe pink dot.
 * 3) If threatened by ghosts -> prefer a green dot (power pellet).
 * 4) Always avoid dangerous cells near non-eatable ghosts.
 *
 * Implementation uses BFS each move.
 */
public class Ex3Algo implements PacManAlgo {

    private int _count;

    // Cached color-int codes (computed once)
    private boolean _init = false;
    private int PINK, BLACK, GREEN;

    // Tunables
    private static final int DANGER_RADIUS_NEAR = 2;
    private static final int DANGER_RADIUS_FAR  = 3;
    private static final int TRIGGER_GREEN_IF_GHOST_WITHIN = 4;
    private static final int MAX_BFS = 20_000; // safety cap

    public Ex3Algo() { _count = 0; }

    @Override
    public String getInfo() {
        return "BFS PacMan: avoid non-eatable ghosts (danger map), eat pink dots, "
                + "use green pellets when threatened, and chase eatable ghosts during power mode.";
    }

    @Override
    public int move(PacmanGame game) {
        final int code = 0;
        _count++;

        int[][] board = game.getGame(code);
        if (board == null || board.length == 0 || board[0].length == 0) return Game.UP;

        initColorsOnce(code);

        P pac = asP(game.getPos(code));
        if (pac == null) return Game.UP;

        GhostCL[] ghosts = game.getGhosts(code);
        if (ghosts == null) ghosts = new GhostCL[0];

        // Optional debug prints
        if (_count == 1 || _count == 300) {
            printBoard(board);
            System.out.println("Pacman coordinate: " + game.getPos(code));
            printGhosts(ghosts);
        }

        boolean[][] passable = buildPassable(board);
        int[][] distToNonEatableGhost = multiSourceBfsDistances(board, passable, ghosts, true);

        // 1) Chase nearest eatable ghost (safe)
        P nextToEatable = nextStepToNearestEatableGhost(board, passable, distToNonEatableGhost, pac, ghosts);
        if (nextToEatable != null && !(nextToEatable.x == pac.x && nextToEatable.y == pac.y)) {
            return dirFromTo(pac, nextToEatable);
        }

        // 2) If threatened -> try go to GREEN
        int minGhostDist = distAt(distToNonEatableGhost, pac);
        boolean threatened = (minGhostDist >= 0 && minGhostDist <= TRIGGER_GREEN_IF_GHOST_WITHIN);

        if (threatened) {
            P nextToGreen = bfsNextStep(board, passable, distToNonEatableGhost, pac,
                    (x, y) -> board[x][y] == GREEN,
                    true);
            if (nextToGreen != null && !(nextToGreen.x == pac.x && nextToGreen.y == pac.y)) {
                return dirFromTo(pac, nextToGreen);
            }
        }

        // 3) Go to nearest safe PINK dot
        P nextToPink = bfsNextStep(board, passable, distToNonEatableGhost, pac,
                (x, y) -> board[x][y] == PINK,
                true);
        if (nextToPink != null && !(nextToPink.x == pac.x && nextToPink.y == pac.y)) {
            return dirFromTo(pac, nextToPink);
        }

        // 4) Escape fallback: maximize distance from ghosts
        int bestDir = bestEscapeDir(board, passable, distToNonEatableGhost, pac);
        if (bestDir != Integer.MIN_VALUE) return bestDir;

        // 5) Final fallback
        return randomDir();
    }

    /* ----------------------------- Init & maps ----------------------------- */

    private void initColorsOnce(int code) {
        if (_init) return;
        PINK  = Game.getIntColor(Color.PINK, code);
        BLACK = Game.getIntColor(Color.BLACK, code);
        GREEN = Game.getIntColor(Color.GREEN, code);
        _init = true;
    }

    private boolean[][] buildPassable(int[][] board) {
        int w = board.length;
        int h = board[0].length;
        boolean[][] pass = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                pass[x][y] = (board[x][y] != BLACK);
            }
        }
        return pass;
    }

    /* ----------------------------- Targets ----------------------------- */

    private P nextStepToNearestEatableGhost(int[][] board,
                                            boolean[][] passable,
                                            int[][] distToNonEatableGhost,
                                            P pac,
                                            GhostCL[] ghosts) {

        boolean[][] target = new boolean[board.length][board[0].length];
        boolean found = false;

        for (GhostCL g : ghosts) {
            if (g == null) continue;
            if (safeRemainEatable(g, 0) > 0) {
                P gp = asP(g.getPos(0));
                if (gp != null && inBounds(board, gp.x, gp.y)) {
                    target[gp.x][gp.y] = true;
                    found = true;
                }
            }
        }
        if (!found) return null;

        return bfsNextStep(board, passable, distToNonEatableGhost, pac,
                (x, y) -> target[x][y],
                true);
    }

    /* ----------------------------- BFS ----------------------------- */

    @FunctionalInterface
    private interface CellPredicate {
        boolean test(int x, int y);
    }

    private P bfsNextStep(int[][] board,
                          boolean[][] passable,
                          int[][] distToNonEatableGhost,
                          P start,
                          CellPredicate isTarget,
                          boolean avoidDanger) {

        int w = board.length;
        int h = board[0].length;

        if (isTarget.test(start.x, start.y)) return start;

        P[][] prev = new P[w][h];
        boolean[][] visited = new boolean[w][h];

        ArrayDeque<P> q = new ArrayDeque<>();
        q.add(new P(start.x, start.y));
        visited[start.x][start.y] = true;

        int explored = 0;

        while (!q.isEmpty()) {
            P cur = q.poll();
            explored++;
            if (explored > MAX_BFS) break;

            if (!(cur.x == start.x && cur.y == start.y) && isTarget.test(cur.x, cur.y)) {
                return reconstructNextStep(start, cur, prev);
            }

            for (P nb : neighbors(cur)) {
                int nx = nb.x, ny = nb.y;
                if (!inBounds(board, nx, ny)) continue;
                if (visited[nx][ny]) continue;
                if (!passable[nx][ny]) continue;

                if (avoidDanger && isDangerous(distToNonEatableGhost, nx, ny, board)) continue;

                visited[nx][ny] = true;
                prev[nx][ny] = cur;
                q.add(new P(nx, ny));
            }
        }

        return null;
    }

    private P reconstructNextStep(P start, P goal, P[][] prev) {
        P cur = goal;
        P p = prev[cur.x][cur.y];
        if (p == null) return null;

        while (p != null && !(p.x == start.x && p.y == start.y)) {
            cur = p;
            p = prev[cur.x][cur.y];
        }
        return cur;
    }

    /* ----------------------------- Danger logic ----------------------------- */

    private boolean isDangerous(int[][] distToNonEatableGhost, int x, int y, int[][] board) {
        if (distToNonEatableGhost == null) return false;
        int d = distToNonEatableGhost[x][y];
        if (d < 0) return false;

        int radius = (isCorridor(board, x, y) ? DANGER_RADIUS_FAR : DANGER_RADIUS_NEAR);
        return d <= radius;
    }

    private boolean isCorridor(int[][] board, int x, int y) {
        int pass = 0;
        for (P nb : neighbors(new P(x, y))) {
            if (inBounds(board, nb.x, nb.y) && board[nb.x][nb.y] != BLACK) pass++;
        }
        return pass <= 2;
    }

    private int bestEscapeDir(int[][] board, boolean[][] passable, int[][] distToNonEatableGhost, P pac) {
        int bestDir = Integer.MIN_VALUE;
        int bestScore = Integer.MIN_VALUE;

        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};

        for (int d : dirs) {
            P np = step(pac, d);
            if (!inBounds(board, np.x, np.y)) continue;
            if (!passable[np.x][np.y]) continue;
            if (isDangerous(distToNonEatableGhost, np.x, np.y, board)) continue;

            int score = distAt(distToNonEatableGhost, np);
            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }

        if (bestDir == Integer.MIN_VALUE) {
            for (int d : dirs) {
                P np = step(pac, d);
                if (!inBounds(board, np.x, np.y)) continue;
                if (!passable[np.x][np.y]) continue;

                int score = distAt(distToNonEatableGhost, np);
                if (score > bestScore) {
                    bestScore = score;
                    bestDir = d;
                }
            }
        }

        return bestDir;
    }

    /* ----------------------------- Ghost distance map ----------------------------- */

    private int[][] multiSourceBfsDistances(int[][] board, boolean[][] passable, GhostCL[] ghosts, boolean onlyNonEatable) {
        int w = board.length;
        int h = board[0].length;
        int[][] dist = new int[w][h];
        for (int[] row : dist) Arrays.fill(row, -1);

        ArrayDeque<P> q = new ArrayDeque<>();

        for (GhostCL g : ghosts) {
            if (g == null) continue;

            boolean eatable = safeRemainEatable(g, 0) > 0;
            if (onlyNonEatable && eatable) continue;

            P gp = asP(g.getPos(0));
            if (gp == null || !inBounds(board, gp.x, gp.y)) continue;

            dist[gp.x][gp.y] = 0;
            q.add(new P(gp.x, gp.y));
        }

        while (!q.isEmpty()) {
            P cur = q.poll();
            int cd = dist[cur.x][cur.y];

            for (P nb : neighbors(cur)) {
                int nx = nb.x, ny = nb.y;
                if (!inBounds(board, nx, ny)) continue;
                if (!passable[nx][ny]) continue;
                if (dist[nx][ny] != -1) continue;

                dist[nx][ny] = cd + 1;
                q.add(new P(nx, ny));
            }
        }

        return dist;
    }

    private int distAt(int[][] dist, P p) {
        if (dist == null || p == null) return -1;
        if (p.x < 0 || p.y < 0 || p.x >= dist.length || p.y >= dist[0].length) return -1;
        return dist[p.x][p.y];
    }

    private int safeRemainEatable(GhostCL g, int code) {
        try {
            return (int) g.remainTimeAsEatable(code);
        } catch (Exception e) {
            return 0;
        }
    }


    /* ----------------------------- Geometry & directions ----------------------------- */

    private static class P {
        final int x, y;
        P(int x, int y) { this.x = x; this.y = y; }
    }

    private static P[] neighbors(P p) {
        return new P[] {
                new P(p.x, p.y - 1),
                new P(p.x - 1, p.y),
                new P(p.x, p.y + 1),
                new P(p.x + 1, p.y)
        };
    }

    private static P step(P p, int dir) {
        if (dir == Game.UP) return new P(p.x, p.y - 1);
        if (dir == Game.DOWN) return new P(p.x, p.y + 1);
        if (dir == Game.LEFT) return new P(p.x - 1, p.y);
        if (dir == Game.RIGHT) return new P(p.x + 1, p.y);
        return new P(p.x, p.y);
    }

    private static int dirFromTo(P from, P to) {
        if (to.x == from.x && to.y == from.y - 1) return Game.UP;
        if (to.x == from.x && to.y == from.y + 1) return Game.DOWN;
        if (to.x == from.x - 1 && to.y == from.y) return Game.LEFT;
        if (to.x == from.x + 1 && to.y == from.y) return Game.RIGHT;
        return Game.UP;
    }

    private static boolean inBounds(int[][] board, int x, int y) {
        return x >= 0 && y >= 0 && x < board.length && y < board[0].length;
    }

    /* ----------------------------- Robust pos parsing ----------------------------- */

    private static P asP(Object posObj) {
        if (posObj == null) return null;
        int[] xy = extractTwoInts(posObj.toString());
        return (xy == null) ? null : new P(xy[0], xy[1]);
    }

    private static int[] extractTwoInts(String s) {
        if (s == null) return null;
        Matcher m = Pattern.compile("-?\\d+").matcher(s);
        int[] out = new int[2];
        int i = 0;
        while (m.find() && i < 2) out[i++] = Integer.parseInt(m.group());
        return (i == 2) ? out : null;
    }

    /* ----------------------------- Debug helpers ----------------------------- */

    private static void printBoard(int[][] b) {
        for (int y = 0; y < b[0].length; y++) {
            for (int x = 0; x < b.length; x++) {
                int v = b[x][y];
                System.out.print(v + "\t");
            }
            System.out.println();
        }
    }

    private static void printGhosts(GhostCL[] gs) {
        for (int i = 0; i < gs.length; i++) {
            GhostCL g = gs[i];
            System.out.println(i + ") status: " + g.getStatus()
                    + ",  type: " + g.getType()
                    + ",  pos: " + g.getPos(0)
                    + ",  time: " + g.remainTimeAsEatable(0));
        }
    }

    private static int randomDir() {
        int[] dirs = {Game.UP, Game.LEFT, Game.DOWN, Game.RIGHT};
        int ind = (int) (Math.random() * dirs.length); // <-- important cast
        return dirs[ind];
    }
}
