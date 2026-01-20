package server;

import exe.ex3.game.Game;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class LevelLoaderEx3 {

    private static final int CODE = 0;

    public static final int WALL  = Game.getIntColor(Color.BLUE,  CODE);
    public static final int DOT   = Game.getIntColor(Color.PINK,  CODE);
    public static final int POWER = Game.getIntColor(Color.GREEN, CODE);
    public static final int EMPTY = 0;

    public static int[][] loadFromFile(String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        if (img == null) throw new IOException("Could not read image: " + path);
        return decode(img);
    }

    public static int[][] loadFromResource(String resourcePath) throws IOException {
        try (InputStream in = LevelLoaderEx3.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IOException("Could not decode image: " + resourcePath);
            return decode(img);
        }
    }

    private static int[][] decode(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][] board = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                board[x][y] = classify(img.getRGB(x, y));
            }
        }
        return board;
    }

    private static int classify(int rgb) {
        Color c = new Color(rgb, true);
        if (c.getAlpha() < 10) return EMPTY;
        if (isNear(c, Color.BLACK, 30)) return WALL;
        if (isNear(c, Color.PINK, 60))  return DOT;
        if (isNear(c, Color.GREEN, 60)) return POWER;
        return EMPTY;
    }

    private static boolean isNear(Color a, Color t, int tol) {
        return Math.abs(a.getRed() - t.getRed()) <= tol
                && Math.abs(a.getGreen() - t.getGreen()) <= tol
                && Math.abs(a.getBlue() - t.getBlue()) <= tol;
    }
}
