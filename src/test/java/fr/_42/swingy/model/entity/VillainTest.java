package fr._42.swingy.model.entity;

import fr._42.swingy.model.map.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class VillainTest {

    /* ------------------------------------------------------------------ */
    /*  Fixtures                                                           */
    /* ------------------------------------------------------------------ */

    private static Villain rat() {
        return new Villain("Giant Rat", 20, 3, 1, new Position(2, 4));
    }

    /* ================================================================== */
    /*  Construction                                                       */
    /* ================================================================== */

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void storesEveryFieldAsGiven() {
            Villain v = new Villain("Orc Warrior", 70, 10, 5, new Position(3, 7));

            assertThat(v.getName()).isEqualTo("Orc Warrior");
            assertThat(v.getHitPoints()).isEqualTo(70);
            assertThat(v.getAttack()).isEqualTo(10);
            assertThat(v.getDefense()).isEqualTo(5);
            assertThat(v.getPosition()).isEqualTo(new Position(3, 7));
        }

        @Test
        void positionIsStoredByReference() {
            // Villain doesn't copy the Position. If this ever changes to a
            // defensive copy, this test surfaces the change deliberately.
            Position p = new Position(1, 1);
            Villain v = new Villain("Rat", 10, 1, 0, p);
            assertThat(v.getPosition()).isSameAs(p);
        }

        @Test
        void zeroDefenseIsAllowed() {
            Villain v = new Villain("Glass Cannon", 10, 100, 0, new Position(0, 0));
            assertThat(v.getDefense()).isZero();
        }

        @Test
        void largeStatsAreAllowed() {
            Villain v = new Villain("Demon Prince", 9999, 9999, 9999, new Position(99, 99));
            assertThat(v.getHitPoints()).isEqualTo(9999);
            assertThat(v.getAttack()).isEqualTo(9999);
            assertThat(v.getDefense()).isEqualTo(9999);
        }

        @Test
        void negativeHitPointsAreStoredAsGiven() {
            // No constructor guard today. This test documents the fact so
            // that if a guard is added, we know to update here (and to
            // decide whether the change is deliberate).
            Villain v = new Villain("Broken", -5, 1, 0, new Position(0, 0));
            assertThat(v.getHitPoints()).isEqualTo(-5);
        }

        @Test
        void nullPositionIsStoredAsGiven() {
            // Also unguarded. Documented because the repository relies on
            // this behavior — see GameRepository.fromDto(VillainDto), which
            // calls new Villain(...) only after building a real Position.
            Villain v = new Villain("Rat", 10, 1, 0, null);
            assertThat(v.getPosition()).isNull();
        }

        @Test
        void nullNameIsStoredAsGiven() {
            Villain v = new Villain(null, 10, 1, 0, new Position(0, 0));
            assertThat(v.getName()).isNull();
        }
    }

    /* ================================================================== */
    /*  Damage                                                             */
    /* ================================================================== */

    @Nested
    @DisplayName("takeDamage")
    class Damage {

        @Test
        void reducesHitPointsByAmount() {
            Villain v = rat();
            v.takeDamage(5);
            assertThat(v.getHitPoints()).isEqualTo(15);
        }

        @Test
        void cannotDropBelowZero() {
            Villain v = rat();
            v.takeDamage(v.getHitPoints() + 100);
            assertThat(v.getHitPoints()).isZero();
        }

        @Test
        void damageExactlyEqualToHitPointsDropsToZero() {
            Villain v = rat();
            v.takeDamage(20);
            assertThat(v.getHitPoints()).isZero();
        }

        @Test
        void zeroDamageIsHarmless() {
            Villain v = rat();
            int before = v.getHitPoints();
            v.takeDamage(0);
            assertThat(v.getHitPoints()).isEqualTo(before);
        }

        @Test
        void damageIsIdempotentAtZero() {
            Villain v = rat();
            v.takeDamage(9999);
            v.takeDamage(9999);
            assertThat(v.getHitPoints()).isZero();
        }

        @Test
        void damageAccumulatesAcrossCalls() {
            Villain v = rat();
            v.takeDamage(3);
            v.takeDamage(4);
            v.takeDamage(5);
            assertThat(v.getHitPoints()).isEqualTo(8);
        }

        @Test
        void damageDoesNotAffectAttackOrDefense() {
            Villain v = rat();
            int atk = v.getAttack(), def = v.getDefense();
            v.takeDamage(50);
            assertThat(v.getAttack()).isEqualTo(atk);
            assertThat(v.getDefense()).isEqualTo(def);
        }

        @Test
        void damageDoesNotAffectPosition() {
            Villain v = rat();
            Position before = v.getPosition();
            v.takeDamage(50);
            assertThat(v.getPosition()).isEqualTo(before);
        }
    }

    /* ================================================================== */
    /*  Immutability of identity                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("identity")
    class Identity {

        @Test
        void nameIsImmutable() {
            // No setter exists — verify via reflection that the field is
            // final, so a future refactor can't silently make it mutable
            // without a test failing.
            assertThat(Villain.class.getDeclaredFields())
                    .filteredOn(f -> f.getName().equals("name"))
                    .allMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        }

        @Test
        void attackIsMutable() {
            assertThat(Villain.class.getDeclaredFields())
                    .filteredOn(f -> f.getName().equals("attack"))
                    .noneMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        }

        @Test
        void defenseIsMutable() {
            assertThat(Villain.class.getDeclaredFields())
                    .filteredOn(f -> f.getName().equals("defense"))
                    .noneMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        }

        @Test
        void hitPointsIsMutable() {
            // The one field that must NOT be final.
            assertThat(Villain.class.getDeclaredFields())
                    .filteredOn(f -> f.getName().equals("hitPoints"))
                    .noneMatch(f -> java.lang.reflect.Modifier.isFinal(f.getModifiers()));
        }
    }

    /* ================================================================== */
    /*  Equality                                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        void villainsWithSameFieldsAreNotEqual() {
            // Villain doesn't override equals/hashCode — two "identical"
            // villains are distinct object identities. This matters for
            // GameMap.removeVillain, which relies on identity, not equality.
            Villain a = new Villain("Rat", 20, 3, 1, new Position(2, 4));
            Villain b = new Villain("Rat", 20, 3, 1, new Position(2, 4));

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        void sameInstanceEqualsItself() {
            Villain v = rat();
            assertThat(v).isEqualTo(v);
        }

        @Test
        void identityIsStableAcrossDamage() {
            // Since there's no equals override, identity-based removal in
            // GameMap.removeVillain works even after the villain has taken
            // damage. Verify by checking reference equality survives.
            Villain v = rat();
            Villain ref = v;
            v.takeDamage(5);
            assertThat(v).isSameAs(ref);
        }
    }

    /* ================================================================== */
    /*  toString                                                           */
    /* ================================================================== */

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        void defaultToStringIncludesClassName() {
            // Villain has no toString override. It inherits Object's,
            // which starts with the class name and an @ sign. Nothing
            // depends on this format, so the test just documents it.
            assertThat(rat().toString()).contains("Villain@");
        }
    }

    /* ================================================================== */
    /*  Battle-relevant invariants                                         */
    /* ================================================================== */

    @Nested
    @DisplayName("battle invariants")
    class BattleInvariants {

        @Test
        void fullHpVillainIsAlive() {
            assertThat(rat().getHitPoints()).isPositive();
        }

        @Test
        void damageBelowHpLeavesVillainAlive() {
            Villain v = rat();
            v.takeDamage(v.getHitPoints() - 1);
            assertThat(v.getHitPoints()).isOne();
        }

        @ParameterizedTest(name = "damage {0} leaves {1} hp")
        @CsvSource({
                " 0, 20",
                " 1, 19",
                "10, 10",
                "19,  1",
                "20,  0",
                "21,  0",
                "99,  0",
        })
        void damageMatrix(int damage, int expectedHp) {
            Villain v = rat();
            v.takeDamage(damage);
            assertThat(v.getHitPoints()).isEqualTo(expectedHp);
        }

        @Test
        void zeroHpMeansDefeated() {
            // BattleSimulator checks villainHp > 0 to decide the winner.
            // Verify the contract: 0 = dead, 1 = alive.
            Villain v = rat();
            v.takeDamage(v.getHitPoints());
            assertThat(v.getHitPoints()).isZero();
        }
    }
}