package fr._42.swingy.model.map;

import fr._42.swingy.model.enums.Direction;

import java.util.Objects;

/**
 * Immutable (x, y) coordinate on the map.
 * Origin (0, 0) is top-left; x grows east, y grows south.
 */
public final class Position {

    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    /** Returns a new Position shifted one step in the given direction. */
    public Position translate(Direction dir, int steps) {
        return switch (dir) {
            case NORTH -> new Position(x, y - steps);
            case SOUTH -> new Position(x, y + steps);
            case EAST  -> new Position(x + steps, y);
            case WEST  -> new Position(x - steps, y);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position)) return false;
        Position p = (Position) o;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}