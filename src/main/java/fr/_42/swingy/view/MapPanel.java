package fr._42.swingy.view;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.map.GameMap;
import fr._42.swingy.model.map.Position;

import javax.swing.*;
import java.awt.*;

public class MapPanel extends JPanel {

    // Classpath-relative paths. Files live under resources/assets/.
    private static final String HERO_IMG    = "/assets/hero.png";
    private static final String VILLAIN_IMG = "/assets/villain.png";
    private static final String WALL_IMG    = "/assets/wall.png";
    private static final String FLOOR_IMG   = "/assets/floor.png";

    private GameMap map;
    private Hero hero;

    public MapPanel() {
        setBackground(Color.BLACK);
    }

    public void setState(GameMap map, Hero hero) {
        System.out.println("[MapPanel] setState map=" + map
            + " size=" + (map != null ? map.getSize() : -1)
            + " hero=" + hero);
        this.map = map;
        this.hero = hero;

        // Preferred size is computed here, NOT in paintComponent.
        if (map != null) {
            int tile = TileImages.TILE_SIZE;
            setPreferredSize(new Dimension(map.getSize() * tile,
                                           map.getSize() * tile));
            revalidate();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (map == null) {
            // Debug aid: if you see this color, the panel IS painting.
            g.setColor(Color.MAGENTA);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        int size = map.getSize();
        int tile = TileImages.TILE_SIZE;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Position p = new Position(x, y);
                int px = x * tile;
                int py = y * tile;

                if (map.isBorder(p)) {
                    g2.drawImage(TileImages.get(WALL_IMG, Color.DARK_GRAY, "#"),
                                px, py, null);
                } else {
                    // Rotate the floor tile deterministically per cell so it
                    // doesn't shimmer between repaints.
                    int rotation = (x * 31 + y * 17) & 3;   // 0..3, stable per (x,y)
                    g2.drawImage(
                        TileImages.getRotated(FLOOR_IMG, Color.GREEN, ".", rotation),
                        px, py, null);
                }

                if (map.getVillainAt(p) != null) {
                    g2.drawImage(TileImages.get(VILLAIN_IMG, Color.RED, "V"),
                                px, py, null);
                }

                if (hero != null && hero.getPosition() != null
                        && hero.getPosition().equals(p)) {
                    g2.drawImage(TileImages.get(HERO_IMG, Color.CYAN, "H"),
                                px, py, null);
                }
            }
        }
    }
}