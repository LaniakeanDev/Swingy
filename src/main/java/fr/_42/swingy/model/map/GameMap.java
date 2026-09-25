package fr._42.swingy.model.map;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Square grid that holds villains and tracks the hero's position.
 *
 * <p>Cell contents:</p>
 * <ul>
 *   <li>{@code null}   → empty tile</li>
 *   <li>{@link Hero}   → the hero</li>
 *   <li>{@link Villain}→ a villain</li>
 * </ul>
 */
public class GameMap {

    private final int size;
    private final Object[][] grid;              // Object so it can hold Hero or Villain
    private final List<Villain> villains = new ArrayList<>();
    private final Random random = new Random();

    public GameMap(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Map size must be positive, got " + size);
        }
        this.size = size;
        this.grid = new Object[size][size];
    }

    /* ------------------------------------------------------------------ */
    /*  Getters                                                            */
    /* ------------------------------------------------------------------ */

    public int getSize() {
        return size;
    }

    /**
     * @return whatever occupies the cell, or {@code null} if empty.
     *         Callers should {@code instanceof}-check the result.
     */
    public Object getCell(Position p) {
        if (!isInside(p)) {
            return null;
        }
        return grid[p.getY()][p.getX()];
    }

    /* ------------------------------------------------------------------ */
    /*  Movement                                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Computes the position one step away from {@code from} in direction {@code dir}.
     *
     * <p>Does <b>not</b> check bounds — the returned position may be outside the
     * map. The caller is expected to test it with {@link #isBorder(Position)} or
     * {@link #isInside(Position)} to decide whether the move is legal.</p>
     *
     * @param from the origin position (must not be null)
     * @param dir  the direction of movement (must not be null)
     * @return a new Position one step away; never null
     */
    public Position getNextPosition(Position from, Direction dir) {
        if (from == null) {
            throw new IllegalArgumentException("from position must not be null");
        }
        if (dir == null) {
            throw new IllegalArgumentException("direction must not be null");
        }
        return from.translate(dir, 1);
    }

    /* ------------------------------------------------------------------ */
    /*  Placement                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Places the hero at the given position. Overwrites any villain there
     * (caller's responsibility to avoid that).
     */
    public void placeHero(Hero hero, Position p) {
        requireInside(p);
        grid[p.getY()][p.getX()] = hero;
    }

    /**
     * Places a villain at the given position and records it in the list.
     * Refuses to overwrite an occupied cell.
     *
     * @return true if the villain was placed, false if the cell was taken.
     */
    public boolean placeVillain(Villain villain, Position p) {
        requireInside(p);
        if (grid[p.getY()][p.getX()] != null) {
            return false;
        }
        grid[p.getY()][p.getX()] = villain;
        villains.add(villain);
        return true;
    }

    /* ------------------------------------------------------------------ */
    /*  Geometry helpers                                                   */
    /* ------------------------------------------------------------------ */

    /** True if the position is exactly on any of the four borders. */
    public boolean isBorder(Position p) {
        return isInside(p)
            && (p.getX() == 0 || p.getX() == size - 1
             || p.getY() == 0 || p.getY() == size - 1);
    }

    /** True if the position is strictly inside the grid (borders included). */
    public boolean isInside(Position p) {
        return p != null
            && p.getX() >= 0 && p.getX() < size
            && p.getY() >= 0 && p.getY() < size;
    }

    /** The geometric center of the map. */
    public Position center() {
        int mid = size / 2;
        return new Position(mid, mid);
    }

    /* ------------------------------------------------------------------ */
    /*  Random villain generation                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Scatters {@code count} villains on random empty cells.
     * The hero's current cell is always left free.
     */
    public void generateVillains(int count) {
        for (int i = 0; i < count; i++) {
            Position p = randomEmptyPosition();
            if (p == null) {
                return;                     // board is full; stop early
            }
            Villain v = new Villain.VillainBuilder()
                    .name("Villain-" + i)
                    .power(1 + random.nextInt(5))
                    .build();
            placeVillain(v, p);
        }
    }

    /** Returns a random empty cell, or {@code null} if the map is full. */
    private Position randomEmptyPosition() {
        // Try random sampling first; fall back to linear scan if unlucky.
        for (int attempt = 0; attempt < 50; attempt++) {
            Position p = new Position(random.nextInt(size), random.nextInt(size));
            if (grid[p.getY()][p.getX()] == null) {
                return p;
            }
        }
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[y][x] == null) {
                    return new Position(x, y);
                }
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /*  Villain lookup                                                     */
    /* ------------------------------------------------------------------ */

    /** @return the villain at the position, or {@code null} if none. */
    public Villain getVillainAt(Position p) {
        Object cell = getCell(p);
        return (cell instanceof Villain) ? (Villain) cell : null;
    }

    public boolean hasVillainAt(Position p) {
        return getVillainAt(p) != null;
    }

    /** Removes a villain from the map (called after the hero defeats it). */
    public void removeVillain(Villain v) {
        villains.remove(v);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid[y][x] == v) {
                    grid[y][x] = null;
                    return;
                }
            }
        }
    }

    /** Read-only view of all live villains. */
    public List<Villain> getVillains() {
        return List.copyOf(villains);
    }

    /* ------------------------------------------------------------------ */
    /*  Internal guards                                                    */
    /* ------------------------------------------------------------------ */

    private void requireInside(Position p) {
        if (!isInside(p)) {
            throw new IllegalArgumentException(
                "Position " + p + " is outside map of size " + size);
        }
    }
}