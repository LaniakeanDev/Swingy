package fr._42.swingy.model.entity;

import fr._42.swingy.model.map.Position;

import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Curated catalog of villains.
 *
 * <p>Entries are grouped into tiers from weakest to strongest so that
 * {@link #rollForHeroLevel(Random, int, Position)} can pick an opponent
 * appropriate to the hero that will face it.</p>
 */
public final class VillainPool {

    private VillainPool() {
        // utility class — no instantiation
    }

    /* ------------------------------------------------------------------ */
    /*  Tiers                                                              */
    /* ------------------------------------------------------------------ */

    /** Weak tier — appropriate for heroes of level 1–3. */
    private static final List<VillainTemplate> WEAK = List.of(
        new VillainTemplate("Giant Rat",        20,  3,  1),
        new VillainTemplate("Goblin Scout",     30,  5,  2),
        new VillainTemplate("Cave Bat",         18,  4,  1),
        new VillainTemplate("Bandit",           35,  6,  3),
        new VillainTemplate("Wild Boar",        40,  5,  2)
    );

    /** Mid tier — appropriate for heroes of level 4–6. */
    private static final List<VillainTemplate> MID = List.of(
        new VillainTemplate("Orc Warrior",      70, 10,  5),
        new VillainTemplate("Skeleton Knight",  65, 12,  6),
        new VillainTemplate("Dire Wolf",        80, 11,  4),
        new VillainTemplate("Dark Cultist",     60, 13,  5),
        new VillainTemplate("Troll",            95,  9,  7)
    );

    /** Strong tier — appropriate for heroes of level 7+. */
    private static final List<VillainTemplate> STRONG = List.of(
        new VillainTemplate("Wyvern",          140, 18, 10),
        new VillainTemplate("Lich",            130, 20,  9),
        new VillainTemplate("Minotaur Lord",   160, 17, 12),
        new VillainTemplate("Shadow Drake",    150, 19, 11),
        new VillainTemplate("Demon Prince",    180, 22, 13)
    );

    /* ------------------------------------------------------------------ */
    /*  Public API                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * Picks a random villain appropriate to the given hero level and
     * places it at the provided position.
     *
     * @param random    the shared Random source (never pass a fresh one)
     * @param heroLevel the current hero's level (drives tier selection)
     * @param position  where the villain will stand
     * @return a freshly constructed Villain
     */
    public static Villain rollForHeroLevel(Random random, int heroLevel, Position position) {
        VillainTemplate template = templateFor(heroLevel)
                .get(random.nextInt(templateFor(heroLevel).size()));
        return template.toVillain(position);
    }

    /** All villain templates, weakest to strongest. Useful for tests. */
    public static List<VillainTemplate> all() {
        return Stream.of(WEAK, MID, STRONG)
                .flatMap(List::stream)
                .toList();
    }

    /* ------------------------------------------------------------------ */
    /*  Internal helpers                                                   */
    /* ------------------------------------------------------------------ */

    private static List<VillainTemplate> templateFor(int heroLevel) {
        if (heroLevel <= 3) return WEAK;
        if (heroLevel <= 6) return MID;
        return STRONG;
    }

    /* ------------------------------------------------------------------ */
    /*  Template                                                           */
    /* ------------------------------------------------------------------ */

    /**
     * Immutable blueprint for a villain. Kept private to the pool so that
     * callers always get a Villain bound to a concrete Position — never a
     * half-built template.
     */
    public static final class VillainTemplate {

        private final String name;
        private final int hitPoints;
        private final int attack;
        private final int defense;

        private VillainTemplate(String name, int hitPoints, int attack, int defense) {
            this.name      = name;
            this.hitPoints = hitPoints;
            this.attack    = attack;
            this.defense   = defense;
        }

        public String getName()  { return name;  }
        public int getHitPoints(){ return hitPoints; }
        public int getAttack()   { return attack; }
        public int getDefense()  { return defense; }

        /** Materializes a real Villain at the given position. */
        public Villain toVillain(Position position) {
            Villain v = new Villain(name, hitPoints, position);
            v.setAttack(attack);
            v.setDefense(defense);
            return v;
        }
    }
}