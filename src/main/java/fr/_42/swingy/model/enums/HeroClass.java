package fr._42.swingy.model.enums;

public enum HeroClass {
    CULTURE_CITIZEN(1, 2, 3),
    CONTACT_AGENT(1, 2, 3),
    SC_AGENT(1, 2, 3),
    CULTURE_DRONE_CIVILIAN(1, 2, 3),
    CULTURE_DRONE_CONTACT(1, 2, 3),
    CONTRACTOR_AGENT(1, 2, 3),
    CULTURE_REFERER(1, 2, 3);

    private final int baseAttack;
    private final int baseDefense;
    private final int baseHitPoints;

    HeroClass(int baseAttack, int baseDefense, int baseHitPoints) {
        this.baseAttack = baseAttack;
        this.baseDefense = baseDefense;
        this.baseHitPoints = baseHitPoints;
    }

    public int getBaseAttack()    { return this.baseAttack; }
    public int getBaseDefense()   { return this.baseDefense; }
    public int getBaseHitPoints() { return this.baseHitPoints; }

    /**
     * Human-readable label for the UI.
     * The enum's {@link #name()} remains the canonical identifier used
     * for serialization and {@link #fromString(String)}.
     */
    public String displayName() {
        return switch (this) {
            case CULTURE_CITIZEN         -> "Culture Citizen";
            case CONTACT_AGENT           -> "Contact Agent";
            case SC_AGENT                -> "SC Agent";
            case CULTURE_DRONE_CIVILIAN  -> "Culture Drone (Civilian)";
            case CULTURE_DRONE_CONTACT   -> "Culture Drone (Contact)";
            case CONTRACTOR_AGENT        -> "Contractor Agent";
            case CULTURE_REFERER         -> "Culture Referer";
        };
    }

    public static HeroClass fromString(String s) {
        if (s == null) return null;
        for (HeroClass hc : values()) {
            if (hc.name().equalsIgnoreCase(s.trim())
                    || hc.displayName().equalsIgnoreCase(s.trim())) {
                return hc;
            }
        }
        return null;
    }
}