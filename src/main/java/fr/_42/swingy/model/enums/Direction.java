package fr._42.swingy.model.enums;

public enum Direction {
    NORTH, EAST, SOUTH, WEST;

    public static Direction fromString(String direction) {
        switch (direction.toLowerCase()) {
            case "north":
                return NORTH;
            case "east":
                return EAST;
            case "south":
                return SOUTH;
            case "west":
                return WEST;
            default:
                throw new IllegalArgumentException("Invalid direction: " + direction);
        }
    }
}
