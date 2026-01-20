package server;

import exe.ex3.game.Game;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * LevelLoaderEx3 is responsible for loading a Pac-Man level from an image.
 *
 * The image pixels are translated into a 2D int matrix that represents the game board.
 * Each color in the image corresponds to a different cell type (wall, dot, power, empty).
 *
 * Coordinates convention:
 * board[x][y] where (0,0) is the top-left corner of the image.
 */
class LevelLoaderEx3 {

    /** Code used by the Game color encoding */
    private static final int CODE = 0;

    /** Board cell types encoded as integers */
    public static final int WALL  = Game.getIntColor(Color.BLUE,  CODE);
    public static final int DOT   = Game.getIntColor(Color.PINK,  CODE);
    public static final int POWER = Game.getIntColor(Color.GREEN, CODE);
    public static final int EMPTY = 0;

    /**
     * Loads a level from an image file on disk.
     *
     * @param path path to the image file
     * @return 2D board array representing the level
     * @throws IOException if the file cannot be read or decoded
     */
    public static int[][] loadFromFile(String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        if (img == null) throw new IOException("Could not read image: " + path);
        return decode(img);
    }

    /**
     * Loads a level image from the classpath resources.
     *
     * @param resourcePath path to the resource image
     * @return 2D board array representing the level
     * @throws IOException if the resource is missing or invalid
     */
    public static int[][] loadFromResource(String resourcePath) throws IOException {
        try (InputStream in = LevelLoaderEx3.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IOException("Could not decode image: " + resourcePath);
            return decode(img);
        }
    }

    /**
     * Converts the image pixels into a 2D board array.
     *
     * @param img source image
     * @return board[x][y] representation of the image
     */
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

    /**
     * Classifies a single pixel color into a board cell type.
     *
     * Transparent pixels are treated as EMPTY.
     * Colors close to BLACK, PINK, or GREEN are mapped accordingly.
     *
     * @param rgb raw pixel color
     * @return encoded cell type
     */
    private static int classify(int rgb) {
        Color c = new Color(rgb, true);

        if (c.getAlpha() < 10) return EMPTY;
        if (isNear(c, Color.BLACK, 30)) return WALL;
        if (isNear(c, Color.PINK, 60))  return DOT;
        if (isNear(c, Color.GREEN, 60)) return POWER;

        return EMPTY;
    }

    /**
     * Checks if two colors are close to each other within a tolerance.
     *
     * @param a first color
     * @param t target color
     * @param tol allowed difference per RGB channel
     * @return true if colors are similar, false otherwise
     */
    private static boolean isNear(Color a, Color t, int tol) {
        return Math.abs(a.getRed()   - t.getRed())   <= tol
                && Math.abs(a.getGreen() - t.getGreen()) <= tol
                && Math.abs(a.getBlue()  - t.getBlue())  <= tol;
    }
}
