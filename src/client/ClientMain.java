package client;

import exe.ex3.game.Game;
import exe.ex3.game.PacManAlgo;
import exe.ex3.game.PacmanGame;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ClientMain {

    public static void main(String[] args) {
        int level = 4;
        int code = 0;
        int maxSteps = 20_000;

        if (args.length >= 1) level = parseIntOr(args[0], level);
        if (args.length >= 2) maxSteps = parseIntOr(args[1], maxSteps);

        PacManAlgo algo = createAlgo();
        if (algo == null) {
            System.out.println("Could not load Ex3Algo. Make sure Ex3Algo exists (client.Ex3Algo or server.Ex3Algo or Ex3Algo).");
            return;
        }

        System.out.println("Starting Ex3 client. level=" + level + ", maxSteps=" + maxSteps);
        System.out.println("Algo: " + algo.getInfo());

        try {
            if (tryRunWithPlay(level, algo)) return;

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

    private static PacManAlgo createAlgo() {
        String[] candidates = {
                "client.Ex3Algo",
                "server.Ex3Algo",
                "Ex3Algo"
        };

        for (String cn : candidates) {
            try {
                Class<?> c = Class.forName(cn);
                Object obj = c.getDeclaredConstructor().newInstance();
                if (obj instanceof PacManAlgo) return (PacManAlgo) obj;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static boolean tryRunWithPlay(int level, PacManAlgo algo) {
        try {
            Class<?> gameCls = Game.class;
            String[] candidates = {"play", "playGame", "start", "run", "playLevel"};

            for (String name : candidates) {
                Method m = findStaticMethod(gameCls, name, int.class, PacManAlgo.class);
                if (m != null) {
                    System.out.println("Found runner: Game." + name + "(int, PacManAlgo) -> starting...");
                    m.invoke(null, level, algo);
                    return true;
                }
            }

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

    private static PacmanGame createGameInstance(int level) {
        try {
            Class<?> gameCls = Game.class;

            Constructor<?> c1 = findCtor(gameCls, int.class);
            if (c1 != null) {
                Object obj = c1.newInstance(level);
                if (obj instanceof PacmanGame) return (PacmanGame) obj;
            }

            Constructor<?> c0 = findCtor(gameCls);
            if (c0 != null) {
                Object obj = c0.newInstance();
                Method init = findMethod(gameCls, "init", int.class);
                if (init != null) init.invoke(obj, level);
                if (obj instanceof PacmanGame) return (PacmanGame) obj;
            }

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

        Method move1 = findMethod(gc, "move", int.class);
        Method move2 = findMethod(gc, "move", int.class, int.class);

        Method isOver = firstExisting(gc,
                "isGameOver", "isOver", "isDone", "gameOver", "finished", "isFinished");

        System.out.println("Running tick loop with " + gc.getSimpleName() + " ...");

        for (int step = 0; step < maxSteps; step++) {

            int dir = algo.move(game);

            if (move1 != null) {
                move1.invoke(game, dir);
            } else if (move2 != null) {
                boolean ok = tryInvoke(move2, game, dir, code);
                if (!ok) tryInvoke(move2, game, code, dir);
            } else {
                System.out.println("Could not find game.move(...) method. Your jar API is different.");
                break;
            }

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
