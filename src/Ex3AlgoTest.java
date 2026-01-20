import exe.ex3.game.Game;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class Ex3AlgoTest {

    private static final int CODE = 0;

    private static final int UP = Game.UP;
    private static final int LEFT = Game.LEFT;
    private static final int DOWN = Game.DOWN;
    private static final int RIGHT = Game.RIGHT;

    private static boolean legal(int d) {
        return d == UP || d == LEFT || d == DOWN || d == RIGHT;
    }

    @Test
    public void moveReturnsLegalDirection() {
        PacManAlgo algo = new Ex3Algo();
        PacmanGame g = stubGame(boardSimpleRightDot(), "1,1");
        int dir = algo.move(g);
        assertTrue(legal(dir));
    }

    @Test
    public void movesTowardNearestDotOnSimpleBoard() {
        PacManAlgo algo = new Ex3Algo();
        PacmanGame g = stubGame(boardSimpleRightDot(), "1,1");
        int dir = algo.move(g);
        assertEquals(RIGHT, dir);
    }

    @Test
    public void doesNotThrowOnManyCallsSameState() {
        PacManAlgo algo = new Ex3Algo();
        PacmanGame g = stubGame(boardSimpleRightDot(), "1,1");
        for (int i = 0; i < 2000; i++) {
            int dir = algo.move(g);
            assertTrue(legal(dir));
        }
    }

    @Test
    public void choosesOnlyOpenNeighborWhenCorridor() {
        PacManAlgo algo = new Ex3Algo();
        PacmanGame g = stubGame(boardCorridorOnlyUp(), "1,1");
        int dir = algo.move(g);
        assertEquals(DOWN, dir);
    }

    private static PacmanGame stubGame(int[][] board, String pacPos) {
        ClassLoader cl = PacmanGame.class.getClassLoader();
        return (PacmanGame) Proxy.newProxyInstance(
                cl,
                new Class[]{PacmanGame.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("getGame")) return board;
                    if (name.equals("getPos")) return pacPos;
                    if (name.equals("getGhosts")) return null;
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) return false;
                    if (rt == int.class) return 0;
                    if (rt == long.class) return 0L;
                    if (rt == double.class) return 0.0;
                    if (rt == float.class) return 0f;
                    if (rt == short.class) return (short) 0;
                    if (rt == byte.class) return (byte) 0;
                    if (rt == char.class) return (char) 0;
                    return null;
                }
        );
    }

    private static int[][] boardSimpleRightDot() {
        int OBS = Game.getIntColor(Color.BLUE, CODE);
        int DOT = Game.getIntColor(Color.PINK, CODE);

        int w = 5, h = 5;
        int[][] b = new int[w][h];

        for (int x = 0; x < w; x++) {
            b[x][0] = OBS;
            b[x][h - 1] = OBS;
        }
        for (int y = 0; y < h; y++) {
            b[0][y] = OBS;
            b[w - 1][y] = OBS;
        }

        b[2][1] = DOT;
        return b;
    }

    private static int[][] boardCorridorOnlyUp() {
        int OBS = Game.getIntColor(Color.BLUE, CODE);
        int DOT = Game.getIntColor(Color.PINK, CODE);

        int w = 3, h = 3;
        int[][] b = new int[w][h];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                b[x][y] = OBS;
            }
        }

        b[1][1] = 0;
        b[1][0] = DOT;

        return b;
    }
}
