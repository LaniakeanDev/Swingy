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

    public int getBaseAttack() {
        return this.baseAttack;
    }
    public int getBaseDefense() {
        return this.baseDefense;
    }
    public int getBaseHitPoints() {
        return this.baseHitPoints;
    }

    public static HeroClass fromString(String className) {
        for (HeroClass heroClass : HeroClass.values()) {
            if (heroClass.name().equalsIgnoreCase(className)) {
                return heroClass;
            }
        }
        throw new IllegalArgumentException("Invalid hero class: " + className);
    }
}
