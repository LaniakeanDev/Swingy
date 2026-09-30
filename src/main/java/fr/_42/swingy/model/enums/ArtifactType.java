package fr._42.swingy.model.enums;

public enum ArtifactType {
    WEAPON, ARMOR, HELM;

        /**
     * @return true if the given hero class may equip an artifact in this slot.
     */
    public boolean isCompatibleWith(HeroClass heroClass) {
        if (heroClass == null) {
            return false;
        }
        return switch (this) {
            case WEAPON, HELM -> true;                       // anyone
            case ARMOR        -> switch (heroClass) {
                case CONTACT_AGENT, SC_AGENT, DRONE, CONTRACTOR, GCU, GSV -> true;
                case CULTURE_CITIZEN, REFERER                             -> false;
            };
        };
    }

    /**
     * Case-insensitive lookup by name. Returns {@code null} if no match,
     * so callers can distinguish "invalid input" from "valid but unknown".
     */
    public static ArtifactType fromString(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return switch (trimmed.toUpperCase()) {
            case "WEAPON" -> WEAPON;
            case "ARMOR"  -> ARMOR;
            case "HELM"   -> HELM;
            default       -> null;
        };
    }

    /**
     * Human-readable name for display. So the UI can print
     * "Helm" instead of "HELM".
     */
    public String displayName() {
        return switch (this) {
            case WEAPON -> "Weapon";
            case ARMOR  -> "Armor";
            case HELM   -> "Helm";
        };
    }
}
