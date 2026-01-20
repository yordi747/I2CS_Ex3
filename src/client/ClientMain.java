package client;

import exe.ex3.game.Game;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Client runner for Ex3:
 * - Creates your Ex3Algo
 * - Starts the official game (if the jar supports it), otherwise runs a simple tick loop.
 *
 * Works with different API versions by using reflection (so you won't get stuck on "method not found").
 */
public class ClientMain {

    public static void main(String[] args) {
        int level = 4;          // default level
        int code = 0;           // in many versions code=0
        int maxSteps = 20_000;  // safety

        if (args.length >= 1) {
            level = parseIntOr(args[0], level);
        }
        if (args.length >= 2) {
            maxSteps = parseIntOr(args[1], maxSteps);
        }

        PacManAlgo algo = new Ex3Algo();
        System.out.println("Starting Ex3 client. level=" + level + ", maxSteps=" + maxSteps);
        System.out.println("Algo: " + algo.getInfo());

        try {
            // 1) Try: run using a "play" style method if exists (best)
            if (tryRunWithPlay(level, algo)) {
                return;
            }

            // 2) Otherwise: create a game instance and run step loop
            PacmanGame game = createGameInstance(level);
            if (game == null) {
                System.out.println("Could not create a Game instance. Check your jar/API.");
                return;
            }

            runTickLoop(game, algo, code, maxSteps);

        } catch (Exception e) {
            System.out.println("ClientMain failed:");
            e.printStackTrace();
        }
    }

    /* -------------------- Option A: use built-in play/start if provided -------------------- */

    private static boolean tryRunWithPlay(int level, PacManAlgo algo) {
        try {
            Class<?> gameCls = Game.class;

            // Common patterns in course jars:
            // Game.play(level, algo)
            // Game.playGame(level, algo)
            // Game.start(level, algo)
            String[] candidates = {"play", "playGame", "start", "run", "playLevel"};

            for (String name : candidates) {
                Method m = findStaticMethod(gameCls, name, int.class, PacManAlgo.class);
                if (m != null) {
                    System.out.println("Found runner: Game." + name + "(int, PacManAlgo) -> starting...");
                    m.invoke(null, level, algo);
                    return true;
                }
            }

            // Sometimes it's (algo, level)
            for (String name : candidates) {
                Method m = findStaticMethod(gameCls, name, PacManAlgo.class, int.class);
                if (m != null) {
                    System.out.println("Found runner: Game." + name + "(PacManAlgo, int) -> starting...");
                    m.invoke(null, algo, level);
                    return true;
                }
            }

            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Method findStaticMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            Method m = cls.getMethod(name, params);
            return (java.lang.reflect.Modifier.isStatic(m.getModifiers())) ? m : null;
        } catch (Exception e) {
            return null;
        }
    }

    /* -------------------- Option B: create instance and tick loop -------------------- */

    private static PacmanGame createGameInstance(int level) {
        try {
            Class<?> gameCls = Game.class;

            // Try constructor Game(int level)
            Constructor<?> c1 = findCtor(gameCls, int.class);
            if (c1 != null) {
                Object obj = c1.newInstance(level);
                if (obj instanceof PacmanGame) return (PacmanGame) obj;
            }

            // Try constructor Game()
            Constructor<?> c0 = findCtor(gameCls);
            if (c0 != null) {
                Object obj = c0.newInstance();
                // try initLevel(level) if exists
                Method init = findMethod(gameCls, "init", int.class);
                if (init != null) init.invoke(obj, level);

                if (obj instanceof PacmanGame) return (PacmanGame) obj;
            }

            // If Game itself doesn't implement PacmanGame in your jar version,
            // maybe there is a "getGame(int level)" factory:
            Method factory = findStaticMethod(gameCls, "getGame", int.class);
            if (factory != null) {
                Object obj = factory.invoke(null, level);
                if (obj instanceof PacmanGame) return (PacmanGame) obj;
            }

            return null;
        } catch (Exception e) {
            System.out.println("Failed creating Game instance: " + e.getMessage());
            return null;
        }
    }

    private static Constructor<?> findCtor(Class<?> cls, Class<?>... params) {
        try {
            return cls.getConstructor(params);
        } catch (Exception e) {
            return null;
        }
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        try {
            return cls.getMethod(name, params);
        } catch (Exception e) {
            return null;
        }
    }

    private static void runTickLoop(PacmanGame game, PacManAlgo algo, int code, int maxSteps) throws Exception {
        Class<?> gc = game.getClass();

        // Find an instance "move" method on the game object:
        // move(int dir) OR move(int dir, int code) OR move(int code, int dir)
        Method move1 = findMethod(gc, "move", int.class);
        Method move2 = findMethod(gc, "move", int.class, int.class);

        // Try to find a "game over" predicate
        Method isOver = firstExisting(gc,
                "isGameOver", "isOver", "isDone", "gameOver", "finished", "isFinished");

        System.out.println("Running tick loop with " + gc.getSimpleName() + " ...");

        for (int step = 0; step < maxSteps; step++) {

            int dir = algo.move(game);

            // Apply move
            if (move1 != null) {
                move1.invoke(game, dir);
            } else if (move2 != null) {
                // try (dir, code) first; if fails then try (code, dir)
                boolean ok = tryInvoke(move2, game, dir, code);
                if (!ok) tryInvoke(move2, game, code, dir);
            } else {
                System.out.println("Could not find game.move(...) method. Your jar API is different.");
                break;
            }

            // Stop if game ended
            if (isOver != null) {
                Object r = isOver.invoke(game);
                if (r instanceof Boolean && (Boolean) r) {
                    System.out.println("Game ended at step " + step);
                    break;
                }
            }
        }

        System.out.println("Tick loop finished.");
    }

    private static Method firstExisting(Class<?> cls, String... names) {
        for (String n : names) {
            Method m = findMethod(cls, n);
            if (m != null) return m;
        }
        return null;
    }

    private static boolean tryInvoke(Method m, Object target, Object a, Object b) {
        try {
            m.invoke(target, a, b);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { return def; }
    }
}
