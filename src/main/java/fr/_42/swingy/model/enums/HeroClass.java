package fr._42.swingy.model.enums;

public enum HeroClass {
    CULTURE_CITIZEN(10, 2, 300),
    CONTACT_AGENT(10, 2, 300),
    SC_AGENT(10, 2, 300),
    DRONE(10, 2, 300),
    GCU(10, 2, 300),
    GSV(10, 2, 300),
    CONTRACTOR(10, 2, 300),
    REFERER(10, 2, 300);

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
        StringBuilder sb = new StringBuilder();
        for (String word : name().split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (word.length() <= 3 && word.equals(word.toUpperCase())) {
                sb.append(word);                                 // preserve SC, GCU, GSV
            } else {
                sb.append(Character.toUpperCase(word.charAt(0)))
                .append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
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