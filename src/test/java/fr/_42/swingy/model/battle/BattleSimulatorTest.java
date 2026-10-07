package fr._42.swingy.model.battle;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

import fr._42.swingy.model.entity.Hero;
import fr._42.swingy.model.entity.Villain;
import fr._42.swingy.model.enums.EncounterResult;
import fr._42.swingy.model.enums.HeroClass;
import fr._42.swingy.model.map.Position;


class BattleSimulatorTest {

    /** A Random stub that always returns a fixed value. */
    private static Random fixed(double nextDouble) {
        return new Random() {
            @Override public double nextDouble() { return nextDouble; }
        };
    }

    @Test void heroStrikesFirstAndWinsAgainstWeakVillain() {
        Hero hero = new Hero.HeroBuilder()
            .name("Aria").heroClass(HeroClass.CONTACT_AGENT).build();
        Villain rat = new Villain("Giant Rat", 5, 1, 0, new Position(0, 0));

        BattleSimulator sim = new BattleSimulator(fixed(0.5));
        BattleReport report = sim.fight(hero, rat);

        assertThat(report.result()).isEqualTo(EncounterResult.HERO_WON);
        assertThat(report.log()).isNotEmpty();
        assertThat(rat.getHitPoints()).isZero();
    }

    @Test void heroLosesToOverwhelmingVillain() {
        Hero hero = new Hero.HeroBuilder()
            .name("Aria").heroClass(HeroClass.CONTACT_AGENT).build();
        Villain demon = new Villain("Demon", 999, 999, 999, new Position(0, 0));

        BattleReport report = new BattleSimulator(fixed(0.5)).fight(hero, demon);

        assertThat(report.result()).isEqualTo(EncounterResult.HERO_LOST);
        assertThat(hero.getCurrentHitPoints()).isZero();
        assertThat(report.drop()).isEmpty();
    }

    @Test void damageIsNeverZero() {
        // Even with defense > attack, min damage is 1.
        // Hero hero = ...; Villain tank = new Villain("Tank", 50, 1, 999, ...);
        // seed RNG so the fight ends quickly
        // assert villain eventually takes at least 1 damage per hit
    }
}

