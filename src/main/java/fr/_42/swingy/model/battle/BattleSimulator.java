package fr._42.swingy.model.battle;

import fr._42.swingy.model.entity.Artifact;
import fr._42.swingy.model.entity.ArtifactPool;
import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Pure battle mechanics: no view, no input, no printing.
 *
 * <p>Every method is deterministic given the same {@link Random} source, so
 * combat can be unit-tested by seeding the RNG.</p>
 */
public class BattleSimulator {

    private final Random random;

    public BattleSimulator(Random random) {
        this.random = random;
    }

    /**
     * Resolves a duel. The hero strikes first. Whoever drops to 0 HP loses.
     * On victory, XP and (possibly) an artifact are applied to the hero.
     *
     * @return the outcome, plus a log of the exchanges and any loot dropped.
     */
    public BattleReport fight(Hero hero, Villain villain) {
        int heroHp    = hero.getCurrentHitPoints();
        int villainHp = villain.getHitPoints();

        List<String> log = new java.util.ArrayList<>();
        log.add(String.format("Battle begins! Hero %d HP vs Villain %d HP",
                heroHp, villainHp));

        boolean heroTurn = true;
        while (heroHp > 0 && villainHp > 0) {
            if (heroTurn) {
                int dmg = computeDamage(hero.getAttack(), villain.getDefense());
                villainHp -= dmg;
                villain.takeDamage(dmg);
                log.add(String.format("  You hit for %d. Villain HP: %d",
                        dmg, Math.max(0, villainHp)));
            } else {
                int dmg = computeDamage(villain.getAttack(), hero.getDefense());
                heroHp -= dmg;
                hero.takeDamage(dmg);
                log.add(String.format("  Villain hits for %d. Your HP: %d",
                        dmg, Math.max(0, heroHp)));
            }
            heroTurn = !heroTurn;
        }
        if (heroHp > 0) {
            Optional<Artifact> drop = awardVictory(hero, villain, log);
            return new BattleReport(EncounterResult.HERO_WON, log, drop);
        }
        return new BattleReport(EncounterResult.HERO_LOST, log, Optional.empty());
    }

    /** damage = max(1, attack - defense/2) * random[0.8, 1.2) */
    private int computeDamage(int attack, int defense) {
        int base = Math.max(1, attack - defense / 2);
        double luck = 0.8 + random.nextDouble() * 0.4;
        return Math.max(1, (int) Math.round(base * luck));
    }

    /**
     * Applies XP and rolls for a drop. The artifact is added to the report's
     * log for the controller to narrate; the artifact itself is returned so
     * the controller can ask the player whether to keep it.
     */
    private Optional<Artifact> awardVictory(Hero hero, Villain villain, List<String> log) {
        long xp = villain.getAttack() * 100L;
        hero.gainExperience(xp);
        log.add(String.format("You gain %d XP. (Level %d, %d XP to next)",
                xp, hero.getLevel(), hero.experienceToNextLevel()));

        if (random.nextDouble() >= 0.60) {
            log.add("No artifact dropped.");
            return Optional.empty();
        }

        Artifact artifact = ArtifactPool.rollForPower(random, villain.getAttack());
        log.add(String.format("The villain dropped: %s (+%d %s).",
                artifact.getName(),
                artifact.getValue(),
                artifact.getType().displayName().toLowerCase()));
        return Optional.of(artifact);
    }
}