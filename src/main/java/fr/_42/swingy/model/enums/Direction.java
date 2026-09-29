package fr._42.swingy.model.enums;

public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    public static Direction fromString(String s) {
        if (s == null) return null;
        return switch (s.trim().toLowerCase()) {
            case "north" -> NORTH;
            case "east"  -> EAST;
            case "south" -> SOUTH;
            case "west"  -> WEST;
            default      -> null;
        };
    }
}
