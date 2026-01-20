package server;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads a Pacman level from an image (png).
 *
 * Convention (can be adjusted):
 * - BLACK   => WALL
 * - WHITE   => EMPTY
 * - PINK    => DOT (pink food)
 * - GREEN   => POWER (green pellet)
 *
 * Output board is int[width][height] (x first, y second) just like your Map class.
 */
public class LevelLoader {

    // Cell codes (you can change these to match your server design)
    public static final int EMPTY = 0;
    public static final int WALL  = 1;
    public static final int DOT   = 2;   // pink
    public static final int POWER = 3;   // green

    /**
     * Load from a file path like "g0.png" or "C:\\...\\g0.png".
     */
    public static int[][] loadFromFile(String path) throws IOException {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path is null/blank");
        }
        BufferedImage img = ImageIO.read(new File(path));
        if (img == null) {
            throw new IOException("Could not read image: " + path);
        }
        return decode(img);
    }

    /**
     * Load from classpath resource (recommended if the png is inside your project/resources).
     * Example: loadFromResource("/g0.png")
     */
    public static int[][] loadFromResource(String resourcePath) throws IOException {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("resourcePath is null/blank");
        }
        try (InputStream in = LevelLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new IOException("Could not decode image: " + resourcePath);
            }
            return decode(img);
        }
    }

    /**
     * Convert the png pixels into board codes.
     * board[x][y] where (0,0) is top-left of the image by default.
     */
    private static int[][] decode(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[][] board = new int[w][h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                board[x][y] = classify(rgb);
            }
        }
        return board;
    }

    /**
     * Decide what each pixel means.
     * If your g0.png uses slightly different shades, we use a "near color" check.
     */
    private static int classify(int rgb) {
        Color c = new Color(rgb, true);

        // ignore transparency -> treat as empty
        if (c.getAlpha() < 10) return EMPTY;

        // Walls (black-ish)
        if (isNear(c, Color.BLACK, 30)) return WALL;

        // Dots (pink-ish)
        if (isNear(c, Color.PINK, 60)) return DOT;

        // Power pellet (green-ish)
        if (isNear(c, Color.GREEN, 60)) return POWER;

        // Empty (white-ish or anything else)
        return EMPTY;
    }

    /**
     * Returns true if color is within 'tol' distance (0..255) per channel from target.
     */
    private static boolean isNear(Color a, Color target, int tol) {
        return Math.abs(a.getRed()   - target.getRed())   <= tol
                && Math.abs(a.getGreen() - target.getGreen()) <= tol
                && Math.abs(a.getBlue()  - target.getBlue())  <= tol;
    }
}
