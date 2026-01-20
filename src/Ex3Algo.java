import exe.ex3.game.Game;
import exe.ex3.game.GhostCL;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.awt.Color;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Ex3Algo
 *
 * Automatic Pac-Man algorithm.
 * The algorithm decides the next move using BFS distance maps.
 * It balances between survival (running away from ghosts)
 * and scoring (eating dots and eatable ghosts).
 */
public class Ex3Algo implements PacManAlgo {

    private static final int CODE = 0;

    // Current game objects (updated every step)
    private GhostCL[] gs;
    private Map world;

    // Counters and chosen direction
    private int stepCount = 0;
    private int chosenDir = Game.UP;

    // Cached paths to avoid recomputing BFS every step
    private Pixel2D[] pathToDots;
    private Pixel2D[] pathToGhost;
    private int pathIdx = 2;

    // Color codes used by the game board
    private final int OBS = Game.getIntColor(Color.BLUE, CODE);
    private final int DOT = Game.getIntColor(Color.PINK, CODE);

    // Direction constants (as provided by the game engine)
    private final int U = Game.UP, L = Game.LEFT, D = Game.DOWN, R = Game.RIGHT;

    /**
     * Returns a short description of the algorithm.
     * This text is shown by the game engine and used for debugging.
     */
    @Override
    public String getInfo() {
        return "Automatic Pac-Man algorithm using BFS distances. "
                + "Runs away from nearby ghosts, eats pink dots when safe, "
                + "and chases ghosts when they are eatable.";
    }

    /**
     * Main decision function of the algorithm.
     * This method is called once every game tick.
     *
     * The function analyzes:
     * - Ghost positions and states
     * - Distances from ghosts
     * - Available dots on the board
     *
     * Based on this data, it chooses the best direction to move.
     */
    @Override
    public int move(PacmanGame game) {

        gs = game.getGhosts(CODE);
        world = new Map(game.getGame(CODE));

        Pixel2D pac = pacPos(game);
        Map danger = new Map(world.getWidth(), world.getHeight(), 0);

        boolean chase = false;
        boolean ignoreGhosts = false;

        // Analyze ghost positions and build danger map
        if (gs != null && gs.length > 0) {

            // If ghosts are eatable, Pac-Man plays aggressively
            if (gs[0].remainTimeAsEatable(CODE) > 0) {
                ignoreGhosts = true;
                chase = true;
                chosenDir = goDots(pac, danger, ignoreGhosts);
            } else {

                // Build distance maps from each ghost
                for (int i = 0; i < gs.length; i++) {
                    Pixel2D gPos = ghostPos(i);
                    Map2D dMap = world.allDistance(gPos, OBS);
                    if (dMap == null) continue;

                    int dp = dMap.getPixel(pac);

                    // Ghost is close enough to be dangerous
                    if (dp < 4 && dp > 0) {

                        // Ghost can be eaten
                        if (gs[i].remainTimeAsEatable(CODE) > 0) {
                            if (dp < 3) {
                                int tmp = goGreen(gPos, pac);
                                if (tmp != -5) {
                                    chosenDir = tmp;
                                    chase = true;
                                }
                            }
                            ignoreGhosts = true;
                        }

                        if (chase || ignoreGhosts) break;

                        // Merge this ghost's distance map into the global danger map
                        mergeMin(danger, dMap);
                    }
                }
            }
        }

        // Final decision: escape or collect dots
        if (!chase && !ignoreGhosts && danger.getPixel(pac) < 4 && danger.getPixel(pac) > 0) {
            chosenDir = flee(pac, danger);
        } else if (!chase) {
            chosenDir = goDots(pac, danger, ignoreGhosts);
        }

        stepCount++;
        return chosenDir;
    }

    /**
     * Moves Pac-Man toward the nearest pink dot.
     *
     * If a cached path exists, the algorithm continues on it.
     * Otherwise, it performs BFS to find the closest dot
     * and moves one step in that direction.
     */
    private int goDots(Pixel2D pac, Map danger, boolean ignore) {

        Map tmp = new Map(world.getMap());

        Pixel2D cached = nextFromCachedPath(pac);
        if (cached != null) return dir(cached, pac);

        // Mark ghost positions as obstacles when needed
        if (!ignore && gs != null) {
            for (int i = 0; i < gs.length; i++) {
                tmp.setPixel(ghostPos(i), OBS);
            }
        }

        Map2D d2 = tmp.allDistance(pac, OBS);
        if (!(d2 instanceof Map)) return flee(pac, danger);

        Pixel2D target = closestByLayers((Map) d2, tmp, DOT, pac);
        if (target != null) {
            pathToDots = tmp.shortestPath(pac, target, OBS);
            pathIdx = 2;
            if (pathToDots != null && pathToDots.length > 1) {
                return dir(pathToDots[1], pac);
            }
        }
        return flee(pac, danger);
    }

    /**
     * Chases a ghost that is currently eatable.
     *
     * A shortest path is calculated to the ghost position.
     * Pac-Man moves one step along that path.
     */
    private int goGreen(Pixel2D ghost, Pixel2D pac) {
        pathToGhost = world.shortestPath(pac, ghost, OBS);
        if (pathToGhost != null && pathToGhost.length > 2) {
            return dir(pathToGhost[1], pac);
        }
        return -5;
    }

    /**
     * Escape behavior.
     *
     * Pac-Man chooses the neighboring cell
     * with the largest distance from ghosts.
     */
    private int flee(Pixel2D pac, Map danger) {
        return dir(bestNeighbor(danger, pac), pac);
    }

    /**
     * Finds the safest neighboring cell.
     *
     * The safest cell is defined as the neighbor
     * with the maximum value in the danger map.
     */
    private Pixel2D bestNeighbor(Map danger, Pixel2D pac) {
        int w = danger.getWidth(), h = danger.getHeight();
        int x = pac.getX(), y = pac.getY();

        Pixel2D best = pac;
        int bestVal = danger.getPixel(pac);

        Pixel2D[] ns = {
                new Index2D((x + 1) % w, y),
                new Index2D((x - 1 + w) % w, y),
                new Index2D(x, (y + 1) % h),
                new Index2D(x, (y - 1 + h) % h)
        };

        for (Pixel2D p : ns) {
            int v = danger.getPixel(p);
            if (v >= bestVal) {
                bestVal = v;
                best = p;
            }
        }
        return best;
    }

    /**
     * Merges ghost distance maps.
     *
     * For each cell, the minimal distance to any ghost is kept.
     * This creates a combined danger map.
     */
    private void mergeMin(Map acc, Map2D oneGhost) {
        int w = acc.getWidth(), h = acc.getHeight();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int d = oneGhost.getPixel(new Index2D(x, y));
                if (d <= 0 || d == OBS) continue;
                int cur = acc.getPixel(x, y);
                if (cur == 0 || d < cur) acc.setPixel(x, y, d);
            }
        }
    }

    /**
     * Finds the closest target using BFS layers.
     *
     * The search expands layer by layer until
     * a cell with the required color is found.
     */
    private Pixel2D closestByLayers(Map dist, Map real, int color, Pixel2D start) {
        int w = dist.getWidth(), h = dist.getHeight();
        boolean cyc = world.isCyclic();

        Queue<Pixel2D> q = new LinkedList<>();
        q.add(start);

        while (!q.isEmpty()) {
            Pixel2D c = q.poll();
            int want = dist.getPixel(c) + 1;
            int x = c.getX(), y = c.getY();

            Pixel2D[] ns = {
                    new Index2D((x + 1) % w, y),
                    new Index2D((x - 1 + w) % w, y),
                    new Index2D(x, (y + 1) % h),
                    new Index2D(x, (y - 1 + h) % h)
            };

            for (Pixel2D p : ns) {
                if (dist.getPixel(p) == want) {
                    if (real.getPixel(p) == color) return p;
                    q.add(p);
                }
            }
        }
        return null;
    }

    /**
     * Continues movement on a cached path.
     *
     * This reduces repeated BFS computations
     * when Pac-Man is already moving toward a target.
     */
    private Pixel2D nextFromCachedPath(Pixel2D pac) {
        if (pathToDots != null && pathIdx < pathToDots.length) {
            if (pathToDots[pathIdx - 1].equals(pac)) {
                return pathToDots[pathIdx++];
            }
        }
        pathIdx = 2;
        pathToDots = null;
        return null;
    }

    /**
     * Parses the ghost position string.
     */
    private Pixel2D ghostPos(int i) {
        String[] a = gs[i].getPos(CODE).split(",");
        return new Index2D(Integer.parseInt(a[0]), Integer.parseInt(a[1]));
    }

    /**
     * Parses Pac-Man position from the game object.
     */
    private Pixel2D pacPos(PacmanGame game) {
        String[] a = game.getPos(CODE).split(",");
        return new Index2D(Integer.parseInt(a[0]), Integer.parseInt(a[1]));
    }

    /**
     * Converts two adjacent cells into a direction constant.
     *
     * Supports cyclic wrapping at the borders of the map.
     */
    private int dir(Pixel2D next, Pixel2D cur) {
        int cx = cur.getX(), cy = cur.getY();
        int nx = next.getX(), ny = next.getY();
        int w = world.getWidth(), h = world.getHeight();

        if ((cx + 1) % w == nx) return R;
        if ((cx - 1 + w) % w == nx) return L;
        if ((cy + 1) % h == ny) return U;
        if ((cy - 1 + h) % h == ny) return D;

        return Game.UP;
    }
}
