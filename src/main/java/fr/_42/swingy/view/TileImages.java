package fr._42.swingy.view;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Loads, caches and scales all sprite images to a fixed tile size. */
public final class TileImages {

    public static final int TILE_SIZE = 40;
    private static final boolean DEBUG = true;

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private TileImages() {}

    /**
     * Tries, in order:
     *   1. classpath resource  (e.g. "/assets/floor.png")
     *   2. filesystem path     (e.g. "assets/floor.png")
     *   3. colored placeholder with a letter
     */
    public static BufferedImage get(String path, Color fallback, String letter) {
        return CACHE.computeIfAbsent(path, p -> load(p, fallback, letter));
    }

    private static BufferedImage load(String path, Color fallback, String letter) {
        BufferedImage src = readClasspath(path);
        if (src == null) src = readFile(path);

        if (src == null) {
            if (DEBUG) System.err.println("[TileImages] NOT FOUND: " + path
                    + " — using placeholder '" + letter + "'");
            return placeholder(fallback, letter);
        }
        if (DEBUG) System.out.println("[TileImages] loaded " + path
                + " (" + src.getWidth() + "x" + src.getHeight() + ")");

        BufferedImage scaled = new BufferedImage(
                TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, TILE_SIZE, TILE_SIZE, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage readClasspath(String path) {
        // Normalize: "/assets/foo.png" regardless of what caller passed
        String res = path.startsWith("/") ? path : "/" + path;
        try (InputStream in = TileImages.class.getResourceAsStream(res)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage readFile(String path) {
        try {
            File f = new File(path);
            return f.exists() ? ImageIO.read(f) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage placeholder(Color color, String letter) {
        BufferedImage img = new BufferedImage(
                TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, TILE_SIZE - 1, TILE_SIZE - 1);
        if (letter != null) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Monospaced", Font.BOLD, TILE_SIZE / 2));
            FontMetrics fm = g.getFontMetrics();
            int x = (TILE_SIZE - fm.stringWidth(letter)) / 2;
            int y = (TILE_SIZE - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(letter, x, y);
        }
        g.dispose();
        return img;
    }

    /** Returns the image rotated by `quarterTurns` * 90 degrees (cached). */
    public static BufferedImage getRotated(String path, Color fallback,
                                        String letter, int quarterTurns) {
        final int turns = ((quarterTurns % 4) + 4) % 4;
        final BufferedImage base = get(path, fallback, letter);  // cache OUTSIDE

        if (turns == 0) return base;

        final String key = path + "#" + turns;
        return CACHE.computeIfAbsent(key, k -> rotate(base, turns));
    }

    private static BufferedImage rotate(BufferedImage src, int quarterTurns) {
        if (quarterTurns == 0) return src;

        int size = src.getWidth();   // assumes square (our tiles are)
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Rotate around the center
        g.rotate(Math.toRadians(quarterTurns * 90), size / 2.0, size / 2.0);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }
}